import SwiftUI
import Combine

private let gold = Color(red: 0.96, green: 0.77, blue: 0.44)
private let mist = Color(red: 0.73, green: 0.77, blue: 0.89)
private let ink = Color(red: 0.043, green: 0.052, blue: 0.105)
private let royal = Color(red: 0.20, green: 0.17, blue: 0.40)
private let letters = ["أ", "ب", "ج", "د"]
private func arabic(_ n: Int) -> String {
    n.formatted(.number.locale(Locale(identifier: "ar_SA")))
}

private enum Page: Hashable { case intro, home, modes, game, stats, settings, rules, resume, results, notebook }
private enum Panel: String, Identifiable { case ladder, aids, call, poll; var id: String { rawValue } }

private extension Mode {
    var symbol: String {
        switch self {
        case .classic: return "crown"
        case .quick: return "bolt"
        case .daily: return "calendar"
        case .expert: return "diamond"
        case .training: return "book"
        case .endless: return "infinity"
        }
    }
    var summary: String {
        switch self {
        case .classic: return "خمسة عشر سؤالًا ومحطتا أمان"
        case .quick: return "أجب عن أكبر عدد قبل نهاية الوقت"
        case .daily: return "مجموعة أسئلة اليوم"
        case .expert: return "طريق المليون بدرجة صعوبة أعلى"
        case .training: return "تعلّم من تفسير كل إجابة"
        case .endless: return "واصل اللعب حتى أول إجابة خاطئة"
        }
    }
}
private extension Aid {
    var symbol: String {
        switch self {
        case .half: return "divide"
        case .audience: return "chart.bar.fill"
        case .friend: return "phone.fill"
        case .swap: return "arrow.triangle.2.circlepath"
        }
    }
}

private struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.scaleEffect(configuration.isPressed ? 0.974 : 1)
            .opacity(configuration.isPressed ? 0.88 : 1)
            .animation(.easeOut(duration: 0.13), value: configuration.isPressed)
    }
}
private struct Diamond: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x:r.midX,y:r.minY)); p.addLine(to: CGPoint(x:r.maxX,y:r.midY))
        p.addLine(to: CGPoint(x:r.midX,y:r.maxY)); p.addLine(to: CGPoint(x:r.minX,y:r.midY))
        p.closeSubpath(); return p
    }
}
private struct Insignia: View {
    var size: CGFloat
    var body: some View {
        ZStack {
            Circle().fill(RadialGradient(colors: [royal, ink], center: .center, startRadius: 1, endRadius: size * 0.5))
            Circle().strokeBorder(gold.opacity(0.8), lineWidth: 1)
            Circle().inset(by: 5).strokeBorder(gold.opacity(0.4), style: StrokeStyle(lineWidth: 2, dash: [1, 5]))
            ForEach(0..<3, id: \.self) { i in
                Diamond().stroke(gold.opacity(i == 0 ? 0.9 : 0.38), lineWidth: 1)
                    .frame(width:size*0.50,height:size*0.63).rotationEffect(.degrees(Double(i)*60))
            }
            Diamond().fill(gold).frame(width:size*0.13,height:size*0.21)
        }.frame(width:size,height:size).shadow(color: gold.opacity(0.18), radius: 14)
        .accessibilityHidden(true)
    }
}
private struct Primary: View {
    var label:String
    var icon:String
    var body: some View {
        HStack {
            Image(systemName:icon); Text(label).fontWeight(.bold); Spacer()
            Image(systemName:"chevron.left").font(.caption.bold())
        }.padding(.horizontal,20).frame(minHeight:54)
            .foregroundStyle(ink)
            .background(LinearGradient(colors:[Color(red:1,green:0.91,blue:0.70),gold],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:16))
    }
}
private extension View {
    func card(_ color: Color = .white.opacity(0.12)) -> some View {
        self.padding(16)
            .background(LinearGradient(colors:[royal.opacity(0.84),ink.opacity(0.96)],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:18))
            .overlay(RoundedRectangle(cornerRadius:18).strokeBorder(color,lineWidth:1))
    }
}
private struct MenuRow: View {
    var title:String
    var detail:String
    var icon:String
    var body: some View {
        HStack(spacing:14) {
            Image(systemName:icon).font(.title3.weight(.medium)).frame(width:40,height:40)
                .foregroundStyle(gold).background(gold.opacity(0.10),in:RoundedRectangle(cornerRadius:12))
            VStack(alignment:.leading,spacing:4) {
                Text(title).font(.headline)
                Text(detail).font(.caption).foregroundStyle(mist)
            }
            Spacer()
            Image(systemName:"chevron.left").font(.caption.bold()).foregroundStyle(mist)
        }.frame(maxWidth:.infinity,alignment:.leading).card()
    }
}
private struct Kicker: View {
    let text:String
    init(_ text:String){self.text=text}
    var body:some View {
        HStack(spacing:7) {
            Diamond().fill(gold).frame(width:7,height:7)
            Text(text).font(.caption.bold()).foregroundStyle(gold)
        }
    }
}

private let teal = Color(red: 0.44, green: 0.88, blue: 0.82)
private let rose = Color(red: 1, green: 0.55, blue: 0.56)

private enum NotebookTab: String, CaseIterable, Identifiable {
    case favorites = "المفضلة"
    case missed = "مراجعة الأخطاء"
    var id: Self { self }
}

private struct SectionTitle: View {
    let title: String
    let subtitle: String
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.title2.bold()).accessibilityAddTraits(.isHeader)
            Text(subtitle).font(.subheadline).foregroundStyle(mist)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ClockStrip: View {
    let remaining: Int
    let total: Int
    let global: Bool
    let active: Bool
    private var tone: Color { active && remaining <= 5 ? rose : teal }
    private var label: String { global ? "وقت التحدي" : "وقت السؤال" }

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "timer").foregroundStyle(tone)
            Text(label).font(.caption.bold())
            ProgressView(value: Double(min(max(0, remaining), max(1,total))), total: Double(max(1,total)))
                .tint(tone)
            Text(arabic(remaining) + " ث").font(.subheadline.bold()).monospacedDigit()
                .foregroundStyle(tone).fixedSize()
        }
        .padding(.horizontal, 14).padding(.vertical, 8)
        .background(ink.opacity(0.93), in: Capsule())
        .overlay(Capsule().strokeBorder(tone.opacity(0.32)))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label).accessibilityValue(arabic(remaining) + " ثانية")
        .accessibilityIdentifier("questionTimer")
    }
}

private struct FeatureButton: View {
    let name: String
    let detail: String
    let image: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 7) {
                Image(systemName: image).font(.title3).foregroundStyle(gold).padding(.bottom, 2)
                Text(name).font(.headline)
                Text(detail).font(.caption).foregroundStyle(mist).lineLimit(2)
            }
            .frame(maxWidth: .infinity, minHeight: 77, alignment: .topLeading)
            .padding(13)
            .background(ink.opacity(0.84), in: RoundedRectangle(cornerRadius: 15))
            .overlay(RoundedRectangle(cornerRadius: 15).strokeBorder(.white.opacity(0.17)))
        }
    }
}

private struct DifficultyLabel: View {
    let value: Int
    private let names = ["تمهيدي", "سهل", "متوسط", "صعب", "متقدم"]
    var body: some View {
        HStack(spacing: 5) {
            HStack(alignment: .bottom, spacing: 2) {
                ForEach(1...5, id: \.self) { step in
                    Capsule().fill(step <= value ? gold : mist.opacity(0.24))
                        .frame(width: 3, height: CGFloat(3 + step * 2))
                }
            }.accessibilityHidden(true)
            Text(names[min(4, max(0, value-1))]).font(.caption2.bold()).foregroundStyle(mist)
        }
    }
}

private struct AnswerNotes: View {
    let question: Question
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(question.explanation).font(.subheadline).foregroundStyle(.white).lineSpacing(3)
            if let url = URL(string: question.source), let scheme = url.scheme, ["https","http"].contains(scheme), let host = url.host {
                Link(destination: url) {
                    Label("المصدر · " + host.replacingOccurrences(of: "www.", with: ""), systemImage: "arrow.up.forward.square")
                        .font(.caption).foregroundStyle(teal).frame(minHeight: 36, alignment: .leading)
                }.accessibilityLabel("فتح مصدر الإجابة · " + host)
            } else if !question.source.isEmpty {
                Text("المصدر · " + question.source).font(.caption).foregroundStyle(mist)
            }
        }
    }
}

private struct NotebookEntry: View {
    let question: Question
    let saved: Bool
    let current: Bool
    let toggle: () -> Void
    @State private var expanded = false
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Kicker(question.category)
                Spacer()
                Button(action: toggle) {
                    Image(systemName: saved ? "bookmark.fill" : "bookmark")
                        .foregroundStyle(gold).frame(width: 44, height: 44)
                }.accessibilityLabel(saved ? "إزالة من المفضلة" : "حفظ في المفضلة")
            }
            Text(question.text).font(.headline).fixedSize(horizontal: false, vertical: true)
            if current {
                Text("هذا السؤال قيد اللعب. سيظهر الشرح بعد كشف الإجابة.").font(.caption).foregroundStyle(mist)
            } else {
                DisclosureGroup("الإجابة والشرح", isExpanded: $expanded) {
                    VStack(alignment: .leading, spacing: 7) {
                        Label(question.options[question.correct], systemImage: "checkmark.seal.fill")
                            .font(.headline).foregroundStyle(teal)
                        AnswerNotes(question: question)
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(.top, 8)
                }.tint(gold)
            }
        }.frame(maxWidth: .infinity, alignment: .leading).card()
    }
}

@MainActor
struct StudioRoot: View {
    @StateObject private var vm = GameVM()
    @StateObject private var sound = GameSound()
    @Environment(\.scenePhase) private var appPhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var page:Page = .intro
    @State private var panel:Panel?
    @State private var withdrawing = false
    @State private var replaceWith:Mode?
    @State private var bankError:String?
    @State private var notebookTab: NotebookTab = .favorites
    @State private var notebookLimit = 20
    @State private var historyLimit = 10
    @State private var lastCountdown: String?
    @State private var lastQuestionAnnouncement: String?
    @State private var lastResultAnnouncement: UUID?
    private let clock = Timer.publish(every:0.5,on:.main,in:.common).autoconnect()

    var body:some View {
        ZStack {
            Color.black.ignoresSafeArea()
            StageView(mood:mood).ignoresSafeArea().allowsHitTesting(false).accessibilityHidden(true)
            LinearGradient(stops:[.init(color:ink.opacity(0.10),location:0),.init(color:ink.opacity(0.38),location:0.34),.init(color:ink.opacity(0.93),location:0.90)],startPoint:.top,endPoint:.bottom).ignoresSafeArea()
            if page == .intro {
                VStack(spacing:15) {
                    Insignia(size:102)
                    Text("من سيربح المليون").font(.largeTitle.bold())
                    Text("استوديو المسابقات").foregroundStyle(gold)
                }.transition(.opacity)
            } else {
                VStack(spacing:0) {
                    bar.padding(.horizontal,20).padding(.vertical,8)
                    if page == .game, let round = vm.round, round.phase != .finished {
                        timer(for: round).padding(.horizontal, 20).padding(.bottom, 3)
                    }
                    ScrollView {
                        VStack(alignment:.leading,spacing:16) { content }
                            .frame(maxWidth:620,alignment:.leading).padding(.horizontal,20).padding(.top,12).padding(.bottom,24)
                            .frame(maxWidth:.infinity)
                    }.id(page).id(page == .game ? vm.round?.index : nil)
                }.transition(.opacity)
            }
        }
        .foregroundStyle(.white).buttonStyle(PressStyle())
        .animation(reduceMotion ? nil : .easeInOut(duration:0.30), value:page)
        .task {
            sound.music(.intro)
            try? await Task.sleep(for:.seconds(1.3))
            if page == .intro { page = .home }
        }
        .onReceive(clock) { _ in
            guard appPhase == .active else { return }
            vm.tick()
            playCountdownIfNeeded()
        }
        .onChange(of:appPhase) { _,newPhase in
            sound.setActive(newPhase == .active)
            if newPhase == .active { vm.tick(); syncMusic() }
        }
        .onChange(of:page) { _, destination in
            if destination != .game { sound.stopAnnouncement() }
            syncMusic()
            if destination == .game { announceQuestionIfNeeded() }
            if destination == .results, let r = vm.round, r.id != lastResultAnnouncement {
                lastResultAnnouncement = r.id
                if r.payout == 1_000_000 { sound.announceCue("win") }
                else if r.timedOut == true { sound.announceCue("timeout") }
                else if r.outcome == "انسحاب" { sound.announceCue("withdraw") }
                else if r.outcome == "إجابة غير صحيحة" { sound.announceCue("wrong") }
                else { sound.announceCue("correct") }
            }
        }
        .onChange(of:vm.question?.id) { _,_ in announceQuestionIfNeeded() }
        .onChange(of:vm.round?.phase) { _,phase in
            guard page == .game || page == .results else { return }
            syncMusic()
            switch phase {
            case .asking: announceQuestionIfNeeded()
            case .locked: sound.stopAnnouncement(); sound.cue(.lock)
            case .revealed:
                panel = nil
                let right = vm.round?.selected == vm.question?.correct
                if right, let r = vm.round, [.classic,.expert].contains(r.mode), [4,9].contains(r.index) {
                    sound.cue(.milestone)
                    sound.announceCue(r.index == 4 ? "safety-1" : "safety-2")
                } else if right {
                    sound.cue(.correct)
                    sound.announceCue("correct")
                } else {
                    sound.cue(.wrong)
                    sound.announceCue(vm.round?.timedOut == true ? "timeout" : "wrong")
                }
            case .finished:
                page = .results
                panel = nil
                withdrawing = false
                if vm.round?.payout == 1_000_000 { sound.cue(.victory); sound.cue(.applause) }
                else if vm.round?.timedOut == true { sound.cue(.wrong) }
            default: break
            }
        }
        .onChange(of:vm.error) { _,message in if let message { bankError = message } }
        .sheet(item:$panel) { sheet in
            ScrollView { VStack(alignment:.leading,spacing:16) {
                HStack { Text(sheet == .ladder ? "سلم الجوائز" : sheet == .call ? "اتصال الاستوديو" : sheet == .poll ? "صوت الجمهور" : "غرفة المساعدة").font(.title2.bold()); Spacer(); Button("إغلاق"){panel=nil} }
                if let round = vm.round, round.phase == .asking { timer(for: round) }
                switch sheet {
                case .ladder: ladder
                case .aids: aids
                case .call: FriendLine(hint:vm.round?.hint ?? "") { panel=nil }
                case .poll: if let votes=vm.round?.votes {AudienceChart(votes:votes)}
                }
            }.padding(22) }
            .foregroundStyle(.white).presentationBackground(ink).presentationDragIndicator(.visible)
            .environment(\.layoutDirection,.rightToLeft).preferredColorScheme(.dark)
        }
        .alert("تأكيد الانسحاب",isPresented:$withdrawing) {
            Button("مواصلة اللعب",role:.cancel){}
            Button("انسحاب",role:.destructive){vm.withdraw()}
        } message:{Text("ستنتهي الجولة وتُسجّل نتيجتك الحالية. المبلغ المحقق " + arabic(vm.round?.earned ?? 0) + " ريال.")}
        .alert("توجد جولة محفوظة",isPresented:Binding(get:{replaceWith != nil},set:{if !$0 {replaceWith=nil}}),presenting:replaceWith) { mode in
            Button("متابعة الجولة"){ page = .resume }
            Button("بدء جولة بديلة",role:.destructive){start(mode)}
            Button("إلغاء",role:.cancel){}
        } message:{_ in Text("يمكنك استكمال الجولة الحالية أو استبدالها بجولة جديدة.")}
        .alert("تنبيه الاستوديو",isPresented:Binding(get:{bankError != nil},set:{if !$0 {bankError=nil}})){
            Button("حسنًا"){bankError=nil}
        } message:{ Text(bankError ?? "") }
    }

    private var mood:StageMood {
        if page == .intro {return .intro}
        if page == .results && vm.round?.payout == 1_000_000 {return .victory}
        if page == .results && vm.round?.timedOut == true {return .wrong}
        guard page == .game, let r = vm.round else {return .calm}
        if r.phase == .locked { return .suspense }
        if r.phase == .revealed {
            if r.selected != vm.question?.correct {return .wrong}
            return [4,9].contains(r.index) ? .milestone : .correct
        }
        return r.index >= 8 ? .tense : .calm
    }
    private func syncMusic() {
        switch page {
        case .intro: sound.music(.intro)
        case .game:
            if vm.round?.phase == .locked {sound.music(.suspense)}
            else {sound.music(.station(questionNumber: (vm.round?.index ?? 0) + 1))}
        case .results where vm.round?.payout == 1_000_000: sound.music(.victory)
        default: sound.music(.menu)
        }
    }
    private func announceQuestionIfNeeded() {
        guard page == .game, let r = vm.round, r.phase == .asking, let q = vm.question else { return }
        let key = r.id.uuidString + ":" + q.id
        guard lastQuestionAnnouncement != key else { return }
        lastQuestionAnnouncement = key
        sound.cue(.question)
        let prize = r.mode.ladder && GameVM.prizes.indices.contains(r.index) ? GameVM.prizes[r.index] : nil
        sound.announceQuestion(number: r.index + 1, prize: prize)
    }
    private func timer(for round: Round) -> some View {
        ClockStrip(remaining: round.mode == .quick ? vm.secondsLeft : vm.questionSecondsLeft,
                   total: vm.questionTimeLimit, global: round.mode == .quick, active: vm.timingActive)
    }
    private func playCountdownIfNeeded() {
        guard page == .game, vm.timingActive, let round = vm.round, round.phase == .asking else { return }
        let seconds = round.mode == .quick ? vm.secondsLeft : vm.questionSecondsLeft
        guard (1...5).contains(seconds) else { return }
        let key = round.id.uuidString + "-" + String(round.index) + "-" + String(seconds)
        if key != lastCountdown { lastCountdown = key; sound.cue(.countdown) }
    }
    private func start(_ mode:Mode) {
        sound.stopAnnouncement()
        vm.start(mode)
        if let message = vm.error {bankError=message;return}
        guard let r = vm.round else {bankError="تعذّر تحميل الأسئلة.";return}
        page = r.phase == .finished ? .results : .game
    }
    private var bar:some View {
        HStack {
            Insignia(size:32)
            Text("استوديو المليون").font(.subheadline.bold())
            Spacer()
            if page != .home {
                Button {page = .home} label:{
                    Label("الرئيسية",systemImage:"house").font(.caption.bold()).padding(10)
                        .background(ink.opacity(0.8),in:Capsule())
                }.accessibilityIdentifier("home")
            } else {
                Text("المستوى " + arabic(vm.level)).font(.caption.bold()).foregroundStyle(gold)
            }
        }
    }
    @ViewBuilder private var content:some View {
        switch page {
        case .home: home; homeActions
        case .modes: modes
        case .game: game
        case .results: results
        case .resume: resume
        case .rules: rules
        case .settings: settings
        case .stats: stats
        case .notebook: notebook
        case .intro: EmptyView()
        }
    }
    private var home: some View {
        VStack(alignment: .leading, spacing: 7) {
            Kicker("مسابقة الثقافة العامة")
            Text("من سيربح").font(.system(.title, design: .rounded).weight(.semibold))
            Text("المليون").font(.system(size: 64, weight: .heavy, design: .rounded))
                .foregroundStyle(LinearGradient(colors: [.white, gold], startPoint: .top, endPoint: .bottom))
            Text("خمسة عشر سؤالًا. محطّتا أمان. ومليون ريال.")
                .font(.subheadline.weight(.medium)).foregroundStyle(mist)
        }
        .padding(.vertical, 18).frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var homeActions: some View {
        Button {page = .modes} label: { Primary(label: "دخول المسابقة", icon: "play.fill") }
            .accessibilityIdentifier("start")
        if let round = vm.round, round.phase != .finished {
            Button {page = .resume} label: {
                MenuRow(title: "استكمال الجولة", detail: round.mode.title + " · السؤال " + arabic(round.index + 1), icon: "bookmark.fill")
            }.accessibilityIdentifier("resume")
        } else if let round = vm.round, round.phase == .finished {
            Button { page = .results } label: {
                MenuRow(title: "نتيجة الجولة الأخيرة", detail: round.outcome ?? round.mode.title, icon: "flag.checkered")
            }.accessibilityIdentifier("lastResult")
        }
        SectionTitle(title: "مساحة المتسابق", subtitle: "نتائجك، ومعارفك، وإعدادات الاستوديو.")
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 9)], spacing: 9) {
            FeatureButton(name: "سجل المتسابق", detail: "الأوسمة وأفضل النتائج", image: "medal") { page = .stats }
                .accessibilityIdentifier("stats")
            FeatureButton(name: "دفتر المعرفة", detail: "المفضلة ومراجعة الأخطاء", image: "books.vertical") { page = .notebook }
                .accessibilityIdentifier("notebook")
            FeatureButton(name: "غرفة التحكم", detail: "الموسيقى والمؤثرات", image: "slider.horizontal.3") { page = .settings }
                .accessibilityIdentifier("settings")
            FeatureButton(name: "دليل المسابقة", detail: "المهل والجوائز والمساعدات", image: "text.book.closed") { page = .rules }
                .accessibilityIdentifier("rules")
        }
        HStack(spacing: 8) {
            Image(systemName: "square.stack.3d.up")
            Text(arabic(vm.bank.count) + " سؤالًا في بنك المسابقة")
            Spacer()
        }.font(.caption.weight(.medium)).foregroundStyle(mist)
    }
    @ViewBuilder private var modes:some View {
        Kicker("قبل البث")
        Text("اختر نمط اللعب").font(.largeTitle.bold())
        Text("تختلف مدة الإجابة وطريقة احتساب النتيجة باختلاف النمط.").font(.subheadline).foregroundStyle(mist)
        ForEach(Mode.allCases) { mode in
            Button {
                if let r = vm.round,r.phase != .finished {replaceWith = mode}
                else {start(mode)}
            } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Label(mode.title, systemImage: mode.symbol).font(.headline)
                        Spacer()
                        Image(systemName: "chevron.left").font(.caption).foregroundStyle(gold)
                    }
                    Text(mode.summary).font(.subheadline).foregroundStyle(mist)
                    Label(arabic(mode.questionTimeLimit) + (mode == .quick ? " ثانية للجولة" : " ثانية للسؤال"),
                          systemImage: "timer").font(.caption.bold()).foregroundStyle(teal)
                }.frame(maxWidth: .infinity, alignment: .leading).card(mode == .classic ? gold.opacity(0.48) : .white.opacity(0.16))
            }.accessibilityIdentifier("mode." + mode.rawValue)
        }
    }
    @ViewBuilder private var resume:some View {
        if let r = vm.round {
            Kicker("جولتك المحفوظة")
            Text(r.mode.title).font(.largeTitle.bold())
            Text("السؤال " + arabic(r.index+1)).font(.title2)
            Text("الإجابات الصحيحة " + arabic(r.correctCount)).foregroundStyle(mist)
            if r.phase == .asking { timer(for: r) }
            Button {page = r.phase == .finished ? .results : .game} label:{Primary(label:"العودة إلى الاستوديو",icon:"play.fill")}.accessibilityIdentifier("continue")
        }
    }
    @Namespace private var answerSelection
    @ViewBuilder private var game:some View {
        if let r = vm.round, let q = vm.question {
            HStack(alignment:.center) {
                VStack(alignment:.leading,spacing:3) {
                    Kicker(r.mode.title)
                    Text("السؤال " + arabic(r.index+1)).font(.title2.bold())
                }
                Spacer()
                if [.classic,.expert].contains(r.mode), GameVM.prizes.indices.contains(r.index) {
                    Button {panel = .ladder} label:{
                        VStack(alignment:.trailing){Text("قيمة السؤال").font(.caption);Text(arabic(GameVM.prizes[r.index]) + " ر.س").bold().foregroundStyle(gold)}
                    }.accessibilityIdentifier("ladder")
                } else {Text("صحيحة " + arabic(r.correctCount)).foregroundStyle(gold)}
            }
            if r.mode.ladder {
                HStack(spacing:4){
                    ForEach(0..<15,id:\.self){ index in
                        Capsule().fill(index < r.index ? gold : index == r.index ? Color.white : Color.white.opacity(0.17))
                            .frame(maxWidth:.infinity).frame(height:[4,9,14].contains(index) ? 6 : 3)
                    }
                }.accessibilityHidden(true)
            }
            VStack(alignment:.leading,spacing:12) {
                HStack(spacing: 8) {
                    Kicker(q.category)
                    Spacer()
                    DifficultyLabel(value: q.difficulty)
                    Button { vm.toggleFavorite(q.id) } label: {
                        Image(systemName: vm.isFavorite(q.id) ? "bookmark.fill" : "bookmark")
                            .foregroundStyle(gold).frame(width: 44, height: 44)
                    }.accessibilityLabel(vm.isFavorite(q.id) ? "إزالة من المفضلة" : "حفظ السؤال في المفضلة")
                     .accessibilityIdentifier("bookmarkQuestion")
                }
                Text(q.text).font(.title3.weight(.bold)).fixedSize(horizontal:false,vertical:true)
                    .frame(maxWidth:.infinity,alignment:.leading).accessibilityIdentifier("question")
                    .id(q.id).transition(.opacity)
                if r.phase == .locked {HStack {ProgressView().tint(gold);Text("الإجابة النهائية…").font(.subheadline).foregroundStyle(gold)}}
                if r.mode.ladder && [4,9].contains(r.index) {
                    Label(r.phase == .revealed && r.selected == q.correct ? "اجتزت محطة الأمان" : "محطة أمان",systemImage:"shield.fill").font(.caption.bold()).foregroundStyle(gold)
                }
            }.frame(maxWidth:.infinity,alignment:.leading)
                .padding(.horizontal,16).padding(.vertical,12)
                .background(LinearGradient(colors:[Color(red:0.13,green:0.14,blue:0.29),ink],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:20))
                .overlay(RoundedRectangle(cornerRadius:20).strokeBorder(gold.opacity(0.5)))
                .shadow(color: .black.opacity(0.3),radius: 7,y: 4)
            VStack(spacing:8) {
                ForEach(q.options.indices,id:\.self) { i in
                    let revealed = r.phase == .revealed
                    let right = revealed && q.correct == i
                    let wrong = revealed && r.selected == i && !right
                    let color = right ? Color.mint : wrong ? .red : r.selected == i ? gold : mist
                    let hidden = r.hidden.contains(i)
                    Button {
                        vm.choose(i); sound.cue(.select)
                    } label:{
                        HStack(spacing:12) {
                            Text(letters[i]).font(.headline).frame(width:32,height:32).foregroundStyle(color)
                                .background(color.opacity(0.12),in:Circle())
                            Text(hidden ? "إجابة محذوفة" : q.options[i]).font(.body.weight(.medium))
                                .fixedSize(horizontal:false,vertical:true)
                            Spacer()
                            if right || wrong || r.selected == i {
                                Image(systemName:right ? "checkmark.circle.fill" : wrong ? "xmark.circle.fill" : "circle.inset.filled").foregroundStyle(color)
                            }
                        }.padding(.horizontal,12).padding(.vertical,8).frame(minHeight:50)
                            .background(LinearGradient(colors:[ink.opacity(0.96),royal.opacity(0.80)],startPoint:.leading,endPoint:.trailing),in:RoundedRectangle(cornerRadius:13))
                            .overlay(RoundedRectangle(cornerRadius:13).strokeBorder(color.opacity(right || wrong || r.selected == i ? 1 : 0.28),lineWidth:1.5))
                            .overlay {
                                if r.selected == i && r.phase != .revealed {
                                    RoundedRectangle(cornerRadius:13).strokeBorder(gold,lineWidth:2)
                                        .matchedGeometryEffect(id:"chosen",in:answerSelection)
                                }
                            }.animation(reduceMotion ? nil : .spring(response:0.34,dampingFraction:0.82),value:r.selected)
                    }.accessibilityIdentifier("answer." + String(i)).disabled(r.phase != .asking || hidden).opacity(hidden ? 0.37 : 1)
                }
            }
            if r.phase == .revealed {
                VStack(alignment:.leading,spacing:7){
                    Text(r.selected == q.correct ? "إجابة صحيحة" : "الإجابة الصحيحة • " + q.options[q.correct]).font(.headline).foregroundStyle(r.selected == q.correct ? Color.mint : gold)
                    if r.timedOut == true { Text("انتهت مهلة هذا السؤال.").font(.caption.bold()).foregroundStyle(rose) }
                    AnswerNotes(question: q)
                }.frame(maxWidth:.infinity,alignment:.leading).card()
                Button {vm.advance()}label:{Primary(label:"متابعة",icon:"arrow.left.circle.fill")}.accessibilityIdentifier("advance")
            } else if r.phase == .asking {

                if r.selected != nil {
                    Button {vm.confirm()}label:{Primary(label:"تثبيت الإجابة النهائية",icon:"lock.fill")}.accessibilityIdentifier("confirm")
                }
                HStack {
                    Button {panel = .aids}label:{Label("وسائل المساعدة",systemImage:"lifepreserver").frame(minHeight:44)}.accessibilityIdentifier("aids")
                    Spacer()
                    Button("انسحاب"){withdrawing = true}.frame(minHeight:44).accessibilityIdentifier("withdraw")
                }.foregroundStyle(gold).font(.subheadline.bold())
                if let votes = r.votes {AudienceChart(votes:votes)}
                if let hint = r.hint {Label(hint,systemImage:"phone.fill").font(.subheadline).card()}
            }
        }
    }
}
private struct AudienceChart:View{
    let votes:[Int]
    @State private var animated = false
    var body:some View {
        VStack(alignment:.leading,spacing:8){
            Label("تصويت الجمهور",systemImage:"chart.bar.fill").font(.headline)
            ForEach(votes.indices,id:\.self){i in HStack{
                Text(letters[i])
                ProgressView(value:animated ? Double(votes[i]) : 0,total:100).tint(gold)
                Text(arabic(votes[i]) + "٪").monospacedDigit()
            }}
        }.frame(maxWidth:.infinity).card().onAppear{withAnimation(.easeOut(duration:0.6)){animated=true}}
    }
}
private struct FriendLine:View{
    let hint:String
    let close:()->Void
    @State private var connected=false
    var body:some View {
        VStack(spacing:18){
            Image(systemName:connected ? "waveform.circle.fill" : "phone.circle.fill")
                .font(.system(size:64)).foregroundStyle(gold)
            Text(connected ? "سامي · صديقك في الاستوديو" : "جارٍ الاتصال بسامي…").font(.headline)
            if connected {
                Text(hint).font(.title3).multilineTextAlignment(.center)
                Button(action:close){Primary(label:"إنهاء الاتصال",icon:"phone.down.fill")}
            } else { ProgressView().tint(gold) }
        }.frame(maxWidth:.infinity).card().task{
            try? await Task.sleep(for:.milliseconds(850))
            if !Task.isCancelled {withAnimation{connected=true}}
        }
    }
}
private extension StudioRoot {
    @ViewBuilder var aids:some View {
        if let r = vm.round {
            ForEach([Aid.half,.audience,.friend,.swap],id:\.self){a in
                Button {
                    sound.cue(.help); vm.use(a)
                    if a == .friend { panel = .call }
                    else if a == .audience { panel = .poll }
                    else { panel = nil }
                }label:{MenuRow(title:a.title,detail:r.used.contains(a) ? "استُخدمت في هذه الجولة" : "متاحة الآن",icon:a.symbol)}
                .disabled(r.used.contains(a) || r.phase != .asking)
                .opacity(r.used.contains(a) ? 0.43 : 1)
                .accessibilityIdentifier("aid." + String(describing:a))
            }
        }
    }
    @ViewBuilder private var ladder:some View {
        ForEach(Array(GameVM.prizes.indices.reversed()),id:\.self){i in
            HStack {
                Text(arabic(i+1)).monospacedDigit().frame(width:32)
                if [4,9].contains(i){Label("أمان",systemImage:"shield.fill").font(.caption)}
                if i == vm.round?.index {Text("السؤال الحالي").font(.caption.bold())}
                Spacer();Text(arabic(GameVM.prizes[i]) + " ريال").bold()
            }.padding(8).foregroundStyle(i == vm.round?.index || [4,9,14].contains(i) ? gold : .white)
                .background(i == vm.round?.index ? gold.opacity(0.15) : Color.clear,in:RoundedRectangle(cornerRadius:8))
        }
    }
    @ViewBuilder private var results:some View {
        if let r = vm.round {
            Insignia(size:92)
            Kicker(r.mode.title)
            Text(r.outcome ?? "انتهت الجولة").font(.largeTitle.bold())
            if [.classic,.expert].contains(r.mode) {
                Text(arabic(r.payout) + " ريال").font(.title.bold()).foregroundStyle(gold)
            }
            VStack(alignment:.leading,spacing:9) {
                metric("الأسئلة المجابة", arabic(r.answered))
                metric("الإجابات الصحيحة", arabic(r.correctCount))
                metric("دقة الإجابة", arabic(r.answered == 0 ? 0 : 100*r.correctCount/r.answered) + "٪")
                metric("وسائل المساعدة المستخدمة", arabic(r.used.count))
            }.card()
            if let question = vm.question, r.timedOut == true || (r.phase == .finished && r.selected != nil && r.selected != question.correct) {
                VStack(alignment: .leading, spacing: 7) {
                    Kicker("مراجعة السؤال الأخير")
                    Text(question.text).font(.headline)
                    Label(question.options[question.correct], systemImage: "checkmark.seal.fill").foregroundStyle(teal)
                    AnswerNotes(question: question)
                }.frame(maxWidth:.infinity,alignment:.leading).card()
            }
            Button {page = .modes}label:{Primary(label:"جولة جديدة",icon:"arrow.clockwise")}.accessibilityIdentifier("newRound")
            Button {page = .stats}label:{MenuRow(title:"سجل المتسابق",detail:"عرض الإحصاءات والأوسمة",icon:"medal")}
            Button {notebookTab = .missed; page = .notebook} label:{MenuRow(title:"مراجعة الأخطاء",detail:"الأسئلة التي أخطأت فيها مع شرحها",icon:"books.vertical")}
        }
    }
    @ViewBuilder private var stats: some View {
        Kicker("بطاقة المتسابق")
        Text("الإنجازات والإحصاءات").font(.largeTitle.bold())
        let correct = vm.history.reduce(0) { $0 + $1.correct }
        VStack(alignment:.leading,spacing:10) {
            HStack { Insignia(size:54)
                VStack(alignment:.leading, spacing:4) {
                    Text("المستوى " + arabic(vm.level)).font(.title2.bold())
                    Text(arabic(15 - correct % 15) + " إجابة صحيحة للوصول إلى المستوى التالي")
                        .font(.caption).foregroundStyle(mist)
                }
            }
            ProgressView(value: Double(correct % 15), total: 15).tint(gold)
            Divider()
            metric("الجولات المسجلة", arabic(vm.history.count))
            metric("إجابات صحيحة", arabic(correct))
            metric("أفضل جائزة", arabic(vm.history.map(\.payout).max() ?? 0) + " ريال")
        }.card(gold.opacity(0.38))
        SectionTitle(title: "خزانة الأوسمة", subtitle: arabic(vm.badges.count) + " من خمسة أوسمة")
        let badges = [
            ("البداية", "flag.checkered", "أكمل جولتك الأولى"),
            ("محطة الأمان", "shield.lefthalf.filled", "اجتز السؤال الخامس في جولة جوائز"),
            ("خبير المسرح", "star.circle", "اجتز السؤال العاشر في جولة جوائز"),
            ("المليونير", "crown", "احصد جائزة المليون"),
            ("المنافس الشامل", "circle.grid.2x2", "سجّل نتيجة في كل نمط")
        ]
        VStack(spacing:12) {
            ForEach(badges, id: \.0) { badge in
                let earned = vm.badges.contains(badge.0)
                HStack(spacing:10) {
                    Image(systemName: badge.1).foregroundStyle(earned ? gold : mist)
                        .frame(width:34,height:34).background((earned ? gold : mist).opacity(0.12),in:Circle())
                    VStack(alignment:.leading,spacing:2) {
                        Text(badge.0).font(.subheadline.bold())
                        Text(badge.2).font(.caption).foregroundStyle(mist)
                    }
                    Spacer()
                    Image(systemName: earned ? "checkmark.seal.fill" : "lock")
                        .foregroundStyle(earned ? teal : mist)
                        .accessibilityLabel(earned ? "مكتسب" : "مقفل")
                }.accessibilityElement(children: .combine)
            }
        }.card()
        SectionTitle(title: "أرقامك القياسية", subtitle: "أفضل نتيجة مسجّلة في كل نمط")
        VStack(spacing: 12) {
            ForEach(Mode.allCases) { mode in
                HStack {
                    Label(mode.title,systemImage:mode.symbol).font(.subheadline)
                    Spacer()
                    if let record = vm.bestResult(for:mode) {
                        Text(mode.ladder ? arabic(record.payout) + " ريال" : arabic(record.correct) + " صحيحة")
                            .font(.subheadline.bold()).foregroundStyle(gold)
                    } else { Text("لم تلعب بعد").font(.caption).foregroundStyle(mist) }
                }.accessibilityElement(children: .combine)
            }
        }.card()
        SectionTitle(title: "سجل الجولات", subtitle: "من الأحدث إلى الأقدم")
        if vm.history.isEmpty {
            Text("ستظهر نتائجك هنا بعد انتهاء الجولة الأولى.").foregroundStyle(mist)
        }
        ForEach(vm.history.sorted{$0.date > $1.date}.prefix(historyLimit),id:\.id){entry in
            VStack(alignment:.leading,spacing:7){
                HStack{Text(entry.mode.title).bold();Spacer();Text(entry.date,style:.date).font(.caption)}
                Text(entry.outcome).foregroundStyle(gold)
                Text("صحيحة " + arabic(entry.correct) + " من " + arabic(entry.answered)).font(.caption).foregroundStyle(mist)
            }.card()
        }
        if historyLimit < vm.history.count {
            Button("عرض جولات أقدم") { historyLimit += 10 }.foregroundStyle(gold).padding(8)
        }
    }

    @ViewBuilder private var notebook: some View {
        Kicker("أرشيف المتسابق")
        Text("دفتر المعرفة").font(.largeTitle.bold())
        Picker("عرض الأسئلة",selection:$notebookTab) {
            ForEach(NotebookTab.allCases) { tab in Text(tab.rawValue).tag(tab) }
        }.pickerStyle(.segmented).onChange(of:notebookTab){ _,_ in notebookLimit=20 }
        let questions = notebookTab == .favorites ? vm.favoriteQuestions : vm.reviewQuestions
        if questions.isEmpty {
            VStack(alignment:.leading,spacing:9) {
                Image(systemName:notebookTab == .favorites ? "bookmark" : "arrow.uturn.backward.circle")
                    .font(.title2).foregroundStyle(gold)
                Text(notebookTab == .favorites ? "لم تحفظ سؤالًا بعد" : "لا توجد أخطاء للمراجعة").font(.title3.bold())
                Text(notebookTab == .favorites ?
                     "اضغط علامة الحفظ على بطاقة السؤال لتعود إليه مع شرحه ومصدره." :
                     "عندما تخطئ في سؤال أو تنتهي مهلته، ستجده هنا مع الإجابة الصحيحة.")
                    .font(.subheadline).foregroundStyle(mist)
            }.frame(maxWidth:.infinity,alignment:.leading).card()
        } else {
            Text("عدد الأسئلة: " + arabic(questions.count)).font(.caption.bold()).foregroundStyle(mist)
            LazyVStack(spacing:12) {
                ForEach(questions.prefix(notebookLimit)) { question in
                    NotebookEntry(question: question, saved: vm.isFavorite(question.id),
                                  current: vm.round?.phase != .finished && vm.round?.phase != .revealed && vm.question?.id == question.id) {
                        vm.toggleFavorite(question.id)
                    }
                }
            }
            if notebookLimit < questions.count {
                Button("عرض المزيد") { notebookLimit += 20 }.foregroundStyle(gold).padding(8)
            }
        }
    }
    private func metric(_ title:String,_ value:String)->some View {
        HStack {Text(title).foregroundStyle(mist);Spacer();Text(value).bold()}
    }
    @ViewBuilder private var settings:some View {
        Kicker("غرفة التحكم")
        Text("إعدادات الصوت").font(.largeTitle.bold())
        VStack(alignment: .leading, spacing: 12) {
            Toggle("صوت المذيع فؤاد", isOn: $sound.announcerEnabled).accessibilityIdentifier("announcerToggle")
            Text("تعليق عربي مولّد بالذكاء الاصطناعي، يعمل دون اتصال.").font(.caption).foregroundStyle(mist)
            Slider(value: $sound.announcerVolume, in: 0...1)
                .accessibilityLabel("مستوى صوت المذيع فؤاد").disabled(!sound.announcerEnabled)
            Button("تجربة صوت فؤاد") { sound.announceCue("question-1") }
                .foregroundStyle(gold).disabled(!sound.announcerEnabled)
        }.card().tint(gold)
        VStack(alignment:.leading,spacing:14){
            Toggle("موسيقى الاستوديو",isOn:$sound.musicEnabled).accessibilityIdentifier("musicToggle")
            Slider(value:$sound.musicVolume,in:0...1).accessibilityLabel("مستوى الموسيقى").disabled(!sound.musicEnabled)
            Text("مستوى الموسيقى " + arabic(Int(sound.musicVolume*100)) + "٪").font(.caption)
        }.card().tint(gold)
        VStack(alignment:.leading,spacing:14){
            Toggle("المؤثرات الصوتية",isOn:$sound.effectsEnabled).accessibilityIdentifier("effectsToggle")
            Slider(value:$sound.effectsVolume,in:0...1,onEditingChanged:{editing in if !editing{sound.cue(.select)}})
                .accessibilityLabel("مستوى المؤثرات").disabled(!sound.effectsEnabled)
            Button("تجربة صوت الإجابة الصحيحة"){sound.cue(.correct)}.disabled(!sound.effectsEnabled).foregroundStyle(gold)
        }.card().tint(gold)
    }
    @ViewBuilder private var rules:some View {
        Kicker("دليل المتسابق")
        Text("قواعد المسابقة").font(.largeTitle.bold())
        rule("طريق المليون","في النمط الكلاسيكي تجيب عن خمسة عشر سؤالًا. اختر أحد الأجوبة الأربعة ثم ثبّت اختيارك. قبل التثبيت تستطيع تغيير رأيك.")
        rule("الجائزة المضمونة","إذا أخطأت قبل إتمام السؤال الخامس تنتهي الجولة بصفر. الإجابة الصحيحة عن الخامس تضمن ألف ريال، وعن العاشر تضمن اثنين وثلاثين ألفًا. الانسحاب يمنحك آخر جائزة حققتها.")
        rule("وسائل المساعدة","احذف إجابتين، أو استطلع رأي الجمهور، أو اتصل بصديق الاستوديو، أو استبدل السؤال. تُستخدم كل وسيلة مرة في الجولة. الجمهور والصديق يقدمان تقديرًا قد يصيب أو يخطئ.")
        rule("مهلة الإجابة","التحدي السريع له دقيقة للجولة كلها. في بقية الأنماط لكل سؤال مهلة موضحة عند اختيار النمط. يتوقف عدّاد السؤال عند تثبيت الإجابة. استخدام مساعدة أو مغادرة التطبيق لا يوقف الوقت.")
        rule("إذا انتهى الوقت","في جولات الجوائز تنتهي الجولة عند آخر محطة أمان. ينتهي التحدي المتواصل أيضًا، بينما يعرض التدريب والتحدي اليومي الإجابة وشرحها قبل المتابعة.")
        rule("استكمال الجولة","اختر «استكمال الجولة» من الرئيسية. إذا مضت المهلة في غيابك، تُحتسب النتيجة عند عودتك.")
        rule("دفتر المعرفة","احفظ أي سؤال بعلامة الحفظ. يجمع دفتر المعرفة أسئلتك المفضلة والأسئلة التي أخطأت فيها مع الإجابات والشروح والمصادر.")

        rule("الجوائز","الريال داخل اللعبة وحدة افتراضية لحساب النتيجة.")
    }
    private func rule(_ title:String,_ body:String)->some View {
        VStack(alignment:.leading,spacing:7){Text(title).font(.headline).foregroundStyle(gold);Text(body).font(.subheadline).lineSpacing(4)}
            .frame(maxWidth:.infinity,alignment:.leading).card()
    }
}
