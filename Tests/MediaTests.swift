import AVFoundation
import XCTest
@testable import Millionaire

final class MediaTests: XCTestCase {
    @MainActor
    func testBundledScoreAndEffectsOpenOnAppleDecoder() async throws {
        let names = Set(Millionaire.MusicTrack.allCases.map(\.rawValue) + SoundCue.allCases.map(\.file))
        XCTAssertEqual(names.count, Millionaire.MusicTrack.allCases.count + SoundCue.allCases.count)
        for name in names.sorted() {
            let url = try XCTUnwrap(Bundle.main.url(forResource: name, withExtension: "m4a", subdirectory: "Audio") ??
                Bundle.main.url(forResource: name, withExtension: "m4a"), name)
            let decoder = try AVAudioPlayer(contentsOf: url)
            XCTAssertGreaterThan(decoder.duration, 0.08, name)
            XCTAssertEqual(decoder.numberOfChannels, 2, name)
            XCTAssertTrue(decoder.prepareToPlay(), name)
        }
    }
    func testQuestionGroupsHaveIndependentMusic() {
        let expected: [Millionaire.MusicTrack] = [.stage1, .stage2, .stage3, .stage4, .stage5]
        for (index, track) in expected.enumerated() {
            for question in (index * 3 + 1)...(index * 3 + 3) {
                XCTAssertEqual(Millionaire.MusicTrack.station(questionNumber: question), track)
            }
        }
        XCTAssertEqual(Millionaire.MusicTrack.station(questionNumber: 16), .stage5)
    }

    func testPresenterUsesArabicOrdinalsAndVirtualWinnings() {
        XCTAssertEqual(AnnouncerScript.question(number: 1, prize: nil), "السؤال الأول.")
        XCTAssertTrue(AnnouncerScript.question(number: 5, prize: 1000).contains("السؤال الخامس"))
        XCTAssertTrue(AnnouncerScript.question(number: 15, prize: 1_000_000).contains("مليون ريال"))
        XCTAssertFalse(AnnouncerScript.question(number: 16, prize: nil).contains("ريال"))
    }

    @MainActor
    func testMusicResumesAfterForegroundAndDelayedSuspensionNotice() async throws {
        let controller = GameSound()
        let initial = controller.musicEnabled
        defer { controller.setActive(false); controller.musicEnabled = initial }
        controller.musicEnabled = true
        controller.music(.stage2)
        XCTAssertTrue(controller.isMusicPlaying)
        controller.setActive(false)
        XCTAssertFalse(controller.isMusicPlaying)
        controller.setActive(true)
        NotificationCenter.default.post(name: AVAudioSession.interruptionNotification, object: AVAudioSession.sharedInstance(), userInfo: [
            AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue,
            AVAudioSessionInterruptionWasSuspendedKey: true
        ])
        try await Task.sleep(for: .milliseconds(180))
        XCTAssertTrue(controller.isMusicPlaying)
    }

    @MainActor
    func testSoundSettingsSurviveControllerRecreation() async {
        let original = GameSound()
        let initial = (original.musicEnabled, original.effectsEnabled, original.musicVolume, original.effectsVolume)
        let presenter = (original.announcerEnabled, original.announcerVolume)
        defer {
            original.announcerEnabled = presenter.0
            original.announcerVolume = presenter.1
            original.musicEnabled = initial.0
            original.effectsEnabled = initial.1
            original.musicVolume = initial.2
            original.effectsVolume = initial.3
        }
        original.announcerEnabled.toggle()
        original.announcerVolume = 0.67
        original.musicEnabled.toggle()
        original.effectsEnabled.toggle()
        original.musicVolume = 0.34
        original.effectsVolume = 0.45
        let restored = GameSound()
        XCTAssertEqual(restored.announcerEnabled, !presenter.0)
        XCTAssertEqual(restored.announcerVolume, 0.67, accuracy: 0.001)
        XCTAssertEqual(restored.musicEnabled, !initial.0)
        XCTAssertEqual(restored.effectsEnabled, !initial.1)
        XCTAssertEqual(restored.musicVolume, 0.34, accuracy: 0.001)
        XCTAssertEqual(restored.effectsVolume, 0.45, accuracy: 0.001)
    }
}
