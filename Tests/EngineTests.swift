import XCTest
@testable import Millionaire

final class EngineTests: XCTestCase {
    private final class Clock {
        var date = Date(timeIntervalSince1970: 1_790_000_000)
        func step(_ seconds: Double = 2) { date = date.addingTimeInterval(seconds) }
    }

    private func questions() -> [Question] {
        (0..<125).map { i in
            Question(id: "fixture-\(i)", text: "سؤال \(i)", options: ["واحد","اثنان","ثلاثة","أربعة"],
                     correct: i % 4, difficulty: 1 + i % 5, category: "اختبار", explanation: "شرح", source: "fixture")
        }
    }

    @MainActor private func play(_ vm: GameVM, clock: Clock, correct: Bool) throws {
        let key = try XCTUnwrap(vm.question?.correct)
        vm.choose(correct ? key : (key + 1) % 4)
        vm.confirm()
        XCTAssertEqual(vm.round?.phase, .locked)
        vm.choose((key + 2) % 4)
        clock.step()
        vm.tick()
        XCTAssertEqual(vm.round?.phase, .revealed)
        vm.advance()
    }

    @MainActor func testMillionAndBalancedSlots() async throws {
        let clock = Clock()
        let vm = try GameVM(questions: questions(), now: { clock.date })
        vm.start(.classic)
        var counts = [0,0,0,0]
        var seen = Set<String>()
        for i in 0..<15 {
            let q = try XCTUnwrap(vm.question)
            XCTAssertTrue(seen.insert(q.id).inserted)
            XCTAssertEqual(q.difficulty, i / 3 + 1)
            counts[q.correct] += 1
            try play(vm, clock: clock, correct: true)
            XCTAssertEqual(vm.round?.earned, GameVM.prizes[i])
        }
        XCTAssertLessThanOrEqual(counts.max()! - counts.min()!, 1)
        XCTAssertEqual(vm.round?.payout, 1_000_000)
        XCTAssertEqual(vm.history.count, 1)
        vm.advance()
        vm.withdraw()
        XCTAssertEqual(vm.history.count, 1)
        XCTAssertTrue(vm.badges.contains("المليونير"))
    }

    @MainActor func testLosingAtSafetyBoundaries() async throws {
        let clock = Clock()
        let vm = try GameVM(questions: questions(), now: { clock.date })
        for (wins, floor) in [(0,0),(4,0),(5,1000),(9,1000),(10,32000),(14,32000)] {
            vm.start(.classic)
            for _ in 0..<wins { try play(vm, clock: clock, correct: true) }
            try play(vm, clock: clock, correct: false)
            XCTAssertEqual(vm.round?.payout, floor)
            XCTAssertEqual(vm.history.last?.answered, wins + 1)
        }
    }

    @MainActor func testWithdrawTakesCurrentPrize() async throws {
        let clock = Clock()
        let vm = try GameVM(questions: questions(), now: { clock.date })
        vm.start(.classic)
        for _ in 0..<4 { try play(vm, clock: clock, correct: true) }
        vm.withdraw()
        XCTAssertEqual(vm.round?.payout, 500)
        XCTAssertEqual(vm.history.last?.answered, 4)
    }

    @MainActor func testAllAidsAndSelectionLocks() async throws {
        let vm = try GameVM(questions: questions())
        vm.start(.classic)
        let q = try XCTUnwrap(vm.question)
        vm.use(.audience)
        XCTAssertEqual(vm.round?.votes?.reduce(0,+), 100)
        vm.use(.half)
        let hidden = try XCTUnwrap(vm.round?.hidden)
        XCTAssertEqual(hidden.count, 2)
        XCTAssertFalse(hidden.contains(q.correct))
        for i in hidden {
            vm.choose(i)
            XCTAssertEqual(vm.round?.votes?[i], 0)
            XCTAssertNil(vm.round?.selected)
        }
        vm.use(.friend)
        XCTAssertNotNil(vm.round?.hint)
        vm.use(.swap)
        XCTAssertNotEqual(vm.question?.id, q.id)
        XCTAssertEqual(vm.question?.difficulty, q.difficulty)
        XCTAssertTrue(vm.round?.hidden.isEmpty == true)
        XCTAssertNil(vm.round?.votes)
        XCTAssertEqual(vm.round?.used.count, 4)
        vm.use(.swap)
        XCTAssertEqual(vm.round?.replaced.count, 1)
        vm.confirm()
        XCTAssertEqual(vm.round?.phase, .asking)
    }

    @MainActor func testDailySelectionAndModeRules() async throws {
        let clock = Clock()
        let vm = try GameVM(questions: questions(), now: { clock.date })
        vm.start(.daily)
        let today = vm.round?.deals.map(\.id)
        vm.start(.daily)
        XCTAssertEqual(vm.round?.deals.map(\.id), today)
        clock.step(86400)
        vm.start(.daily)
        XCTAssertNotEqual(vm.round?.deals.map(\.id), today)
        vm.start(.expert)
        XCTAssertTrue((vm.question?.difficulty ?? 0) >= 3)
        vm.start(.endless)
        try play(vm, clock: clock, correct: false)
        XCTAssertEqual(vm.round?.phase, .finished)
        vm.start(.training)
        try play(vm, clock: clock, correct: false)
        XCTAssertEqual(vm.round?.phase, .asking)
        XCTAssertEqual(vm.round?.index, 1)
        vm.start(.quick)
        let q = try XCTUnwrap(vm.question)
        clock.step(59.9)
        vm.choose(q.correct)
        vm.confirm()
        clock.step(0.2)
        vm.tick()
        XCTAssertEqual(vm.round?.phase, .finished)
        XCTAssertEqual(vm.history.last?.correct, 1)
    }

    @MainActor func testReplacementNeverReappearsAcrossWholeDeal() async throws {
        let vm = try GameVM(questions: questions())
        for mode in Mode.allCases {
            vm.start(mode)
            let before = try XCTUnwrap(vm.round)
            let original = try XCTUnwrap(vm.question)
            vm.use(.swap)
            let after = try XCTUnwrap(vm.round)
            let replacement = try XCTUnwrap(vm.question)
            XCTAssertEqual(original.difficulty, replacement.difficulty)
            XCTAssertFalse(before.deals.contains(where: { $0.id == replacement.id }))
            XCTAssertFalse(after.deals.contains(where: { $0.id == original.id }))
            XCTAssertEqual(Set(after.deals.map(\.id)).count, after.total)
            vm.use(.half)
            vm.use(.audience)
            let votes = try XCTUnwrap(vm.round?.votes)
            XCTAssertEqual(votes.reduce(0,+), 100)
            for hidden in vm.round?.hidden ?? [] { XCTAssertEqual(votes[hidden], 0) }
        }
    }

    @MainActor func testInvalidBankRejectedAndUnavailableSwapPreserved() async throws {
        let first = questions()[0]
        XCTAssertThrowsError(try GameVM(questions: [first, first]))
        let vm = try GameVM(questions: [first])
        vm.start(.classic)
        XCTAssertNil(vm.round)
        vm.start(.training)
        vm.use(.swap)
        XCTAssertEqual(vm.question?.id, first.id)
        XCTAssertTrue(vm.round?.used.isEmpty == true)
        XCTAssertNotNil(vm.error)
    }

    @MainActor func testRestoreAcrossLockAndCorruption() async throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = folder.appendingPathComponent("snapshot.json")
        let clock = Clock()
        let vm = try GameVM(questions: questions(), store: url, now: { clock.date })
        vm.start(.classic)
        vm.use(.half)
        vm.choose(try XCTUnwrap(vm.question?.correct))
        vm.confirm()
        clock.step()
        let restored = try GameVM(questions: questions(), store: url, now: { clock.date })
        XCTAssertEqual(restored.round?.phase, .revealed)
        XCTAssertEqual(restored.round?.correctCount, 1)
        XCTAssertEqual(restored.round?.used, [.half])
        restored.advance()
        restored.withdraw()
        let again = try GameVM(questions: questions(), store: url, now: { clock.date })
        XCTAssertEqual(again.history.count, 1)
        XCTAssertEqual(again.history.first?.payout, 100)
        try Data([0,1,0]).write(to: url)
        let recovered = try GameVM(questions: questions(), store: url, now: { clock.date })
        XCTAssertNil(recovered.round)
        XCTAssertNotNil(recovered.error)
        recovered.start(.training)
        XCTAssertNotNil(recovered.question)
    }
}
