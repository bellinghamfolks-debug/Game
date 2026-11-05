import Foundation
import Combine

struct Question: Codable, Identifiable, Equatable {
    let id: String
    let text: String
    let options: [String]
    let correct: Int
    let difficulty: Int
    let category: String
    let explanation: String
    let source: String

    func arranged(_ order: [Int]) -> Question {
        Question(id: id, text: text, options: order.map { options[$0] },
                 correct: order.firstIndex(of: correct)!, difficulty: difficulty,
                 category: category, explanation: explanation, source: source)
    }
}

enum Mode: String, CaseIterable, Codable, Identifiable {
    case classic, quick, daily, expert, training, endless
    var id: Self { self }
    var title: String {
        switch self {
        case .classic: return "المسابقة الكلاسيكية"
        case .quick: return "التحدي السريع"
        case .daily: return "التحدي اليومي"
        case .expert: return "المليون المتقدم"
        case .training: return "التدريب"
        case .endless: return "التحدي المتواصل"
        }
    }
    var questionTimeLimit: Int {
        switch self {
        case .quick: return 60
        case .expert: return 20
        case .training: return 45
        case .classic, .daily, .endless: return 30
        }
    }
    var ladder: Bool { self == .classic || self == .expert }
}

enum Aid: String, CaseIterable, Codable, Identifiable {
    case half, audience, friend, swap
    var id: Self { self }
    var title: String {
        switch self {
        case .half: return "حذف إجابتين"
        case .audience: return "تصويت الجمهور"
        case .friend: return "الاتصال بصديق"
        case .swap: return "تغيير السؤال"
        }
    }
}

enum Phase: String, Codable { case asking, locked, revealed, finished }

struct Deal: Codable {
    let id: String
    let order: [Int]
}

struct Round: Codable, Identifiable {
    let id: UUID
    let mode: Mode
    var deals: [Deal]
    var total: Int { deals.count }
    var index = 0
    var phase: Phase = .asking
    var selected: Int?
    var hidden: [Int] = []
    var used: [Aid] = []
    var votes: [Int]?
    var hint: String?
    var correctCount = 0
    var answered = 0
    var earned = 0
    var deadline: Date?
    var questionDeadline: Date?
    var timedOut: Bool?
    var outcome: String?
    var payout = 0
    var revealAt: Date?
    var replaced: [String] = []
}

struct ResultRecord: Codable, Identifiable {
    let id: UUID
    let mode: Mode
    let date: Date
    let correct: Int
    let answered: Int
    let payout: Int
    let outcome: String
}

private struct Snapshot: Codable {
    let round: Round?
    let history: [ResultRecord]
    let seen: Set<String>?
    let favorites: [String]?
    let missed: [String]?
}

private struct Seeded: RandomNumberGenerator {
    var state: UInt64
    mutating func next() -> UInt64 {
        state &+= 0x9e3779b97f4a7c15
        var n = state
        n = (n ^ (n >> 30)) &* 0xbf58476d1ce4e5b9
        n = (n ^ (n >> 27)) &* 0x94d049bb133111eb
        return n ^ (n >> 31)
    }
}

@MainActor
final class GameVM: ObservableObject {
    static let prizes = [100,200,300,500,1000,2000,4000,8000,16000,32000,64000,125000,250000,500000,1000000]
    @Published private(set) var bank: [Question] = []
    @Published private(set) var round: Round?
    @Published private(set) var history: [ResultRecord] = []
    @Published private(set) var secondsLeft = 0
    @Published private(set) var questionSecondsLeft = 0
    @Published private(set) var favoriteIDs: [String] = []
    @Published private(set) var missedIDs: [String] = []
    private var seen: Set<String> = []
    @Published var error: String?
    private var lookup: [String:Question] = [:]
    private var revealTask: Task<Void, Never>?
    private let store: URL?
    private let now: () -> Date

    var question: Question? {
        guard let r = round, r.deals.indices.contains(r.index) else { return nil }
        let d = r.deals[r.index]
        return lookup[d.id]?.arranged(d.order)
    }
    private func timeLimit(for mode: Mode) -> Int {
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        if args.contains("-ui-testing"), mode != .quick {
            return args.contains("-deadline-test") ? 3 : 300
        }
        #endif
        return mode.questionTimeLimit
    }
    var questionTimeLimit: Int { round.map { timeLimit(for: $0.mode) } ?? 0 }
    var timingActive: Bool {
        guard let r = round else { return false }
        return r.mode == .quick ? r.phase != .finished : r.phase == .asking
    }
    var favoriteQuestions: [Question] { favoriteIDs.compactMap { lookup[$0] } }
    var reviewQuestions: [Question] { missedIDs.compactMap { lookup[$0] } }
    func isFavorite(_ id: String) -> Bool { favoriteIDs.contains(id) }
    func toggleFavorite(_ id: String) {
        guard lookup[id] != nil else { return }
        if isFavorite(id) { favoriteIDs.removeAll { $0 == id } }
        else { favoriteIDs.insert(id, at: 0) }
        save()
    }
    func bestResult(for mode: Mode) -> ResultRecord? {
        history.filter { $0.mode == mode }.max { a,b in
            if mode.ladder && a.payout != b.payout { return a.payout < b.payout }
            if a.correct != b.correct { return a.correct < b.correct }
            return a.date < b.date
        }
    }
    var level: Int { 1 + history.reduce(0) { $0 + $1.correct } / 15 }
    var badges: [String] {
        var names: [String] = []
        if !history.isEmpty { names.append("البداية") }
        if history.contains(where: { $0.mode.ladder && $0.correct >= 5 }) { names.append("محطة الأمان") }
        if history.contains(where: { $0.mode.ladder && $0.correct >= 10 }) { names.append("خبير المسرح") }
        if history.contains(where: { $0.payout == 1000000 }) { names.append("المليونير") }
        if Set(history.map(\.mode)).count == Mode.allCases.count { names.append("المنافس الشامل") }
        return names
    }

    init() {
        store = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first?.appendingPathComponent("Millionaire-v1.json")
        now = Date.init
        do {
            guard let u = Bundle.main.url(forResource: "questions", withExtension: "json") else { throw CocoaError(.fileNoSuchFile) }
            let extra = (Bundle.main.urls(forResourcesWithExtension: "json", subdirectory: nil) ?? [])
                .filter { $0.lastPathComponent.hasPrefix("questionpack-") }
                .sorted { $0.lastPathComponent < $1.lastPathComponent }
            let items = try ([u] + extra).flatMap { file in
                try JSONDecoder().decode([Question].self, from: Data(contentsOf: file))
            }
            try install(items)
            load()
        } catch { self.error = "تعذّر تحميل بنك الأسئلة." }
    }

    init(questions: [Question], store: URL? = nil, now: @escaping () -> Date = Date.init) throws {
        self.store = store
        self.now = now
        try install(questions)
        load()
    }

    private func install(_ items: [Question]) throws {
        guard Set(items.map(\.id)).count == items.count, !items.isEmpty,
              items.allSatisfy({ !$0.text.isEmpty && $0.options.count == 4 && Set($0.options).count == 4 &&
                  (0..<4).contains($0.correct) && (1...5).contains($0.difficulty) }) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        bank = items
        lookup = Dictionary(uniqueKeysWithValues: items.map { ($0.id, $0) })
    }

    func load() {
        guard let store, FileManager.default.fileExists(atPath: store.path) else { return }
        do {
            let s = try JSONDecoder().decode(Snapshot.self, from: Data(contentsOf: store))
            guard s.history.allSatisfy({ (0...bank.count).contains($0.correct) && $0.answered >= $0.correct &&
                $0.payout >= 0 && $0.payout <= 1_000_000 }) else { throw CocoaError(.fileReadCorruptFile) }
            history = s.history
            seen = (s.seen ?? []).intersection(lookup.keys)
            favoriteIDs = (s.favorites ?? []).filter { lookup[$0] != nil }
            missedIDs = (s.missed ?? []).filter { lookup[$0] != nil }
            if let r = s.round {
                guard r.deals.indices.contains(r.index), r.deals.allSatisfy({
                    lookup[$0.id] != nil && $0.order.sorted() == [0,1,2,3]
                }), Set(r.deals.map(\.id)).count == r.total,
                r.selected.map({ (0..<4).contains($0) }) ?? true,
                r.phase != .locked || r.selected != nil,
                r.phase != .revealed || r.selected != nil || r.timedOut == true,
                !r.mode.ladder || r.total == 15,
                (0...r.total).contains(r.answered), (0...r.answered).contains(r.correctCount),
                r.phase != .locked || r.revealAt != nil,
                r.mode != .quick || r.deadline != nil,
                r.hidden.allSatisfy({ (0..<4).contains($0) }),
                r.votes.map({ $0.count == 4 && $0.allSatisfy { (0...100).contains($0) } && $0.reduce(0,+) == 100 }) ?? true
                else { throw CocoaError(.fileReadCorruptFile) }
            }
            round = s.round
            if let r = round {
                // Older saves recorded dealt IDs, but lacked a per-question deadline.
                if s.seen == nil {
                    seen.formUnion(r.deals.prefix(r.index + 1).map(\.id))
                    seen.formUnion(r.replaced)
                }
                if r.phase == .asking && r.mode != .quick && r.questionDeadline == nil {
                    round?.questionDeadline = now().addingTimeInterval(Double(timeLimit(for: r.mode)))
                    save()
                }
            }
            tick()
            if round?.phase == .locked { scheduleReveal() }
        } catch { round = nil; self.error = "تعذّر استعادة الجولة المحفوظة. يمكنك بدء جولة جديدة." }
    }

    private func save() {
        guard let store else { return }
        do {
            try FileManager.default.createDirectory(at: store.deletingLastPathComponent(), withIntermediateDirectories: true)
            let data = try JSONEncoder().encode(Snapshot(round: round, history: history, seen: seen, favorites: favoriteIDs, missed: missedIDs))
            try data.write(to: store, options: .atomic)
        } catch { self.error = "تعذّر حفظ تقدّم الجولة." }
    }

    func start(_ mode: Mode) {
        guard !bank.isEmpty else { error = "بنك الأسئلة غير متاح."; return }
        revealTask?.cancel()
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        let parts = calendar.dateComponents([.year, .month, .day], from: now())
        let year = parts.year ?? 0
        let month = parts.month ?? 0
        let day = parts.day ?? 0
        let daySeed = UInt64(year * 10_000 + month * 100 + day)
        var g = Seeded(state: mode == .daily ? daySeed : UInt64.random(in: .min ... .max))
        // Reserve unseen alternatives at each difficulty for a question swap.
        var pools = (1...5).map { difficulty -> [Question] in
            let tier = bank.filter { $0.difficulty == difficulty }
            var categories = Dictionary(grouping: tier, by: \.category)
            let names = categories.keys.sorted()
            for name in names { categories[name]?.shuffle(using: &g) }
            var active = names
            var scheduled: [Question] = []
            while !active.isEmpty {
                active.shuffle(using: &g)
                for category in active {
                    if let next = categories[category]?.popLast() { scheduled.append(next) }
                }
                active.removeAll { categories[$0]?.isEmpty != false }
            }
            // Daily competitors always receive the same draw. For other modes, keep
            // the category rotation while preferring questions this player has not seen.
            if mode != .daily {
                scheduled = scheduled.filter { !seen.contains($0.id) } + scheduled.filter { seen.contains($0.id) }
            }
            return Array(scheduled.reversed())
        }
        var draw: [Question] = []
        let target: Int
        switch mode {
        case .classic,.expert: target = 15
        case .daily: target = min(10, bank.count)
        case .training: target = min(20, bank.count)
        case .quick,.endless: target = max(1, bank.count - pools.filter { $0.count > 1 }.count)
        }
        for i in 0..<target {
            let tier: Int
            switch mode {
            case .classic: tier = min(4,i/3)
            case .expert: tier = min(4,2+i/5)
            case .quick,.endless: tier = Int.random(in: 0..<5, using: &g)
            default: tier = i % 5
            }
            let tiers = pools.indices.sorted { (left: Int, right: Int) -> Bool in
                let l = abs(left - tier)
                let r = abs(right - tier)
                return l == r ? left < right : l < r
            }
            guard let t = tiers.first(where: { pools[$0].count > 1 }) ?? tiers.first(where: { !pools[$0].isEmpty }) else { break }
            draw.append(pools[t].removeLast())
        }
        guard !draw.isEmpty, !mode.ladder || draw.count == 15 else { error = "عدد الأسئلة المتاحة لا يكفي لهذه الجولة."; return }
        // Balance slots across the whole round, without a predictable four-question cycle.
        var slots = (0..<4).shuffled(using: &g)
        slots = (0..<draw.count).map { slots[$0 % 4] }.shuffled(using: &g)
        let deals = draw.enumerated().map { i,q in Self.deal(q, at: slots[i], using: &g) }
        round = Round(id: UUID(), mode: mode, deals: deals, deadline: mode == .quick ? now().addingTimeInterval(60) : nil)
        beginQuestion()
        error = nil
        tick()
        save()
    }

    private func beginQuestion() {
        guard let r = round else { return }
        seen.insert(r.deals[r.index].id)
        round?.timedOut = nil
        if r.mode != .quick {
            round?.questionDeadline = now().addingTimeInterval(Double(timeLimit(for: r.mode)))
            questionSecondsLeft = timeLimit(for: r.mode)
        }
    }

    private func markMissed(_ id: String) {
        missedIDs.removeAll { $0 == id }
        missedIDs.insert(id, at: 0)
    }

    private func safetyPayout(_ r: Round) -> Int {
        guard r.mode.ladder else { return 0 }
        if r.correctCount >= 10 { return 32_000 }
        return r.correctCount >= 5 ? 1_000 : 0
    }

    private static func deal<R: RandomNumberGenerator>(_ q: Question, at slot: Int, using g: inout R) -> Deal {
        var order = q.options.indices.filter { $0 != q.correct }.shuffled(using: &g)
        order.insert(q.correct, at: slot)
        return Deal(id: q.id, order: order)
    }

    func choose(_ i: Int) {
        tick()
        guard var r = round, r.phase == .asking, (0..<4).contains(i), !r.hidden.contains(i), r.selected != i else { return }
        r.selected = i
        round = r
        save()
    }

    func confirm() {
        tick()
        guard var r = round, r.phase == .asking, r.selected != nil else { return }
        r.phase = .locked
        r.questionDeadline = nil
        r.revealAt = now().addingTimeInterval(r.mode == .quick ? 0.35 : 1.4)
        round = r
        save()
        scheduleReveal()
    }

    private func scheduleReveal() {
        revealTask?.cancel()
        guard let r = round, let time = r.revealAt else { return }
        let delay = min(2, max(0, time.timeIntervalSince(now())))
        revealTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            guard !Task.isCancelled, let self, self.round?.id == r.id, self.round?.index == r.index else { return }
            self.reveal()
        }
    }

    private func reveal() {
        guard var r = round, r.phase == .locked, let q = question else { return }
        r.phase = .revealed
        r.revealAt = nil
        r.answered += 1
        if r.selected == q.correct {
            r.correctCount += 1
            if r.mode.ladder { r.earned = Self.prizes[r.index] }
        } else {
            markMissed(q.id)
        }
        round = r
        save()
    }

    func advance() {
        tick()
        guard var r = round, r.phase == .revealed, let q = question else { return }
        if r.selected != q.correct && (r.mode.ladder || r.mode == .endless) {
            finish("إجابة غير صحيحة", payout: safetyPayout(r))
        } else if r.index + 1 == r.total {
            finish(r.mode.ladder ? "فزت بالمليون!" : "اكتمل التحدي", payout: r.earned)
        } else {
            r.index += 1
            r.phase = .asking
            r.selected = nil
            r.hidden = []
            r.votes = nil
            r.hint = nil
            round = r
            beginQuestion()
            save()
        }
    }

    func withdraw() {
        tick()
        guard let r = round, r.phase == .asking else { return }
        finish("انسحاب", payout: r.earned)
    }

    func tick() {
        guard let r = round, r.phase != .finished else { return }
        if r.phase == .locked, let at = r.revealAt, at <= now() { reveal() }
        if r.mode == .quick, let deadline = r.deadline {
            let left = remaining(until: deadline)
            if secondsLeft != left { secondsLeft = left }
            if questionSecondsLeft != left { questionSecondsLeft = left }
            if left == 0 {
                // A choice confirmed before the bell still counts.
                if round?.phase == .locked { reveal() }
                finish("انتهى الوقت", payout: 0)
            }
        } else {
            if secondsLeft != 0 { secondsLeft = 0 }
            guard r.phase == .asking, let deadline = r.questionDeadline else { return }
            let left = remaining(until: deadline)
            if questionSecondsLeft != left { questionSecondsLeft = left }
            if left == 0 { expireQuestion() }
        }
    }

    private func remaining(until deadline: Date) -> Int {
        Int(max(0, min(Double(questionTimeLimit), ceil(deadline.timeIntervalSince(now())))))
    }

    private func expireQuestion() {
        guard var r = round, r.phase == .asking else { return }
        markMissed(r.deals[r.index].id)
        r.selected = nil
        r.answered += 1
        r.timedOut = true
        r.questionDeadline = nil
        r.phase = .revealed
        round = r
        if r.mode.ladder || r.mode == .endless {
            finish("انتهى وقت السؤال", payout: safetyPayout(r))
        } else {
            save()
        }
    }

    private func finish(_ outcome: String, payout: Int) {
        guard var r = round, r.phase != .finished else { return }
        revealTask?.cancel()
        r.phase = .finished
        r.questionDeadline = nil
        r.outcome = outcome
        r.payout = payout
        history.append(ResultRecord(id: r.id, mode: r.mode, date: now(), correct: r.correctCount,
                                    answered: r.answered, payout: payout, outcome: outcome))
        round = r
        save()
    }

    func use(_ aid: Aid) {
        tick()
        guard var r = round, r.phase == .asking, !r.used.contains(aid), let q = question else { return }
        let wrong = q.options.indices.filter { $0 != q.correct && !r.hidden.contains($0) }
        switch aid {
        case .half:
            r.hidden = Array(wrong.shuffled().prefix(2))
            if let s = r.selected, r.hidden.contains(s) { r.selected = nil }
            if r.votes != nil {
                let remaining = q.options.indices.filter { !r.hidden.contains($0) }
                if let old = r.votes, remaining.count == 2 {
                    let sum = old[remaining[0]] + old[remaining[1]]
                    var adjusted = [0,0,0,0]
                    adjusted[remaining[0]] = sum == 0 ? 50 : (old[remaining[0]] * 100 + sum / 2) / sum
                    adjusted[remaining[1]] = 100 - adjusted[remaining[0]]
                    r.votes = adjusted
                }
            }
            if let hint = r.hint, r.hidden.contains(where: { hint.contains("«" + q.options[$0] + "»") }) { r.hint = nil }
        case .audience:
            r.votes = Self.poll(q, hidden: r.hidden)
        case .friend:
            let confidence = Int.random(in: (83-q.difficulty*5)...(99-q.difficulty*3))
            let guess = Int.random(in: 0..<100) < confidence ? q.correct : (wrong.randomElement() ?? q.correct)
            r.hint = "صديقك يرجّح «" + q.options[guess] + "» بثقة " + String(confidence) + "٪."
        case .swap:
            let excluded = Set(r.deals.map(\.id) + r.replaced)
            let candidates = bank.filter { $0.difficulty == q.difficulty && !excluded.contains($0.id) }
            guard let replacement = candidates.filter({ !seen.contains($0.id) }).randomElement() ?? candidates.randomElement() else {
                error = "لا يتوفر سؤال بديل من المستوى نفسه."; return
            }
            var gen = SystemRandomNumberGenerator()
            seen.insert(replacement.id)
            r.replaced.append(q.id)
            r.deals[r.index] = Self.deal(replacement, at: Int.random(in: 0..<4), using: &gen)
            r.selected = nil
            r.hidden = []
            r.votes = nil
            r.hint = nil
        }
        r.used.append(aid)
        round = r
        save()
    }

    private static func poll(_ q: Question, hidden: [Int]) -> [Int] {
        let active = q.options.indices.filter { !hidden.contains($0) }
        let weights = active.map { $0 == q.correct ? Int.random(in: 40...80) - 5*q.difficulty : Int.random(in: 3...(10+5*q.difficulty)) }
        let total = weights.reduce(0,+)
        var votes = [0,0,0,0]
        for (i, slot) in active.enumerated() { votes[slot] = 100 * weights[i] / total }
        for i in active.shuffled().prefix(100-votes.reduce(0,+)) { votes[i] += 1 }
        return votes
    }
}
