import XCTest

final class GameUITests: XCTestCase {
    private var app: XCUIApplication!

    @MainActor private func launch(deadlineTest: Bool = false) {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = deadlineTest ? ["-ui-testing", "-deadline-test"] : ["-ui-testing"]
        app.launch()
        XCTAssertTrue(app.buttons["start"].waitForExistence(timeout: 10))
    }

    @MainActor private func attachScreen(_ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor private func tap(_ e: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(e.waitForExistence(timeout: 6), file: file, line: line)
        for _ in 0..<7 {
            if e.isHittable {e.tap(); return}
            app.swipeUp()
        }
        XCTFail("Control could not be reached: " + e.description, file:file,line:line)
    }

    @MainActor private func openRound(_ mode:String, waitForQuestion: Bool = true) {
        tap(app.buttons["start"])
        tap(app.buttons["mode." + mode])
        let replace = app.alerts.buttons["بدء جولة بديلة"]
        if replace.waitForExistence(timeout:1) {replace.tap()}
        if waitForQuestion { XCTAssertTrue(app.staticTexts["question"].waitForExistence(timeout:5)) }
    }

    @MainActor func testClassicLadderSaveRestoreHelpAndWithdrawal() {
        launch()
        attachScreen("Home")
        openRound("classic")
        attachScreen("Game")
        let question = app.staticTexts["question"].label
        tap(app.buttons["ladder"])
        XCTAssertTrue(app.staticTexts["سلم الجوائز"].waitForExistence(timeout:4))
        tap(app.buttons["إغلاق"])
        tap(app.buttons["aids"])
        tap(app.buttons["aid.half"])
        let answers = (0..<4).map {app.buttons["answer." + String($0)]}
        XCTAssertEqual(answers.filter(\.isEnabled).count,2)
        app.terminate()
        app.launch()
        tap(app.buttons["resume"])
        tap(app.buttons["continue"])
        XCTAssertEqual(app.staticTexts["question"].label,question)
        XCTAssertEqual(answers.filter(\.isEnabled).count,2)
        tap(app.buttons["withdraw"])
        tap(app.alerts.buttons["انسحاب"])
        XCTAssertTrue(app.buttons["newRound"].waitForExistence(timeout:5))
    }

    @MainActor func testTrainingConfirmsAndReveals() {
        launch()
        openRound("training")
        let question = app.staticTexts["question"].label
        let bookmark = app.buttons["bookmarkQuestion"]
        if bookmark.label != "إزالة من المفضلة" { tap(bookmark) }
        tap(app.buttons["answer.0"])
        tap(app.buttons["confirm"])
        XCTAssertTrue(app.buttons["advance"].waitForExistence(timeout:7))
        tap(app.buttons["advance"])
        XCTAssertTrue(app.buttons["answer.1"].isEnabled)
        tap(app.buttons["home"])
        tap(app.buttons["notebook"])
        XCTAssertTrue(app.staticTexts[question].waitForExistence(timeout: 5))
    }

    @MainActor func testTimeLimitEndsClassicRound() {
        launch(deadlineTest: true)
        openRound("classic", waitForQuestion: false)
        XCTAssertTrue(app.buttons["newRound"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["انتهى وقت السؤال"].exists)
        XCTAssertFalse(app.buttons["confirm"].exists)
    }

    @MainActor func testControlRoomGuideAndStats() {
        launch()
        tap(app.buttons["settings"])
        XCTAssertTrue(app.switches["announcerToggle"].exists)
        let music = app.switches["musicToggle"]
        let original = music.value as? String
        tap(music)
        XCTAssertNotEqual(music.value as? String, original)
        tap(music)
        XCTAssertEqual(music.value as? String, original)
        tap(app.buttons["home"])
        tap(app.buttons["rules"])
        XCTAssertTrue(app.staticTexts["قواعد المسابقة"].exists)
        tap(app.buttons["home"])
        tap(app.buttons["stats"])
        XCTAssertTrue(app.staticTexts["الإنجازات والإحصاءات"].exists)
    }

    @MainActor func testPollFriendAndReplacement() {
        launch()
        openRound("classic")
        let first = app.staticTexts["question"].label
        tap(app.buttons["aids"])
        tap(app.buttons["aid.audience"])
        XCTAssertTrue(app.staticTexts["صوت الجمهور"].waitForExistence(timeout:5))
        tap(app.buttons["إغلاق"])
        tap(app.buttons["aids"])
        tap(app.buttons["aid.friend"])
        tap(app.buttons["إنهاء الاتصال"])
        tap(app.buttons["aids"])
        tap(app.buttons["aid.swap"])
        XCTAssertNotEqual(app.staticTexts["question"].label,first)
    }

    @MainActor func testRemainingModesOpenAQuestion() {
        launch()
        for mode in ["quick","daily","expert","endless"] {
            openRound(mode)
            XCTAssertTrue(app.buttons["answer.3"].exists)
            tap(app.buttons["home"])
        }
    }

    @MainActor func testStartScreenPerformance() {
        launch()
        measure(metrics:[XCTApplicationLaunchMetric()]) { app.launch() }
    }
}
