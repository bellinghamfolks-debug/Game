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

    func testPresenterRoutesEveryQuestionAndGameMode() {
        XCTAssertEqual(PresenterAudio.files.count, 23)
        XCTAssertEqual(Set(PresenterAudio.files).count, 23)
        for number in 1...15 {
            XCTAssertEqual(PresenterAudio.question(number: number, prize: 100), "question-" + String(number))
            XCTAssertEqual(PresenterAudio.question(number: number, prize: nil), "question-generic")
        }
        XCTAssertEqual(PresenterAudio.question(number: 16, prize: 100), "question-generic")
        XCTAssertEqual(PresenterAudio.question(number: 0, prize: 100), "question-generic")
    }

    @MainActor
    func testAllPresenterRecordingsDecode() async throws {
        for name in PresenterAudio.files {
            let url = try XCTUnwrap(Bundle.main.url(forResource: name, withExtension: "m4a", subdirectory: "Audio/Presenter"), name)
            let decoder = try AVAudioPlayer(contentsOf: url)
            XCTAssertGreaterThan(decoder.duration, 0.25, name)
            XCTAssertTrue(decoder.prepareToPlay(), name)
        }
    }

    @MainActor
    func testRecordedPresenterDucksMusicAndStopsInBackground() async throws {
        let sound = GameSound()
        let initial = (sound.announcerEnabled, sound.announcerVolume, sound.musicEnabled, sound.musicVolume)
        defer {
            sound.setActive(false)
            sound.announcerEnabled = initial.0
            sound.announcerVolume = initial.1
            sound.musicEnabled = initial.2
            sound.musicVolume = initial.3
        }
        sound.announcerEnabled = true
        sound.announcerVolume = 0.9
        sound.musicEnabled = true
        sound.musicVolume = 0.6
        sound.music(.stage1)
        sound.announceQuestion(number: 1, prize: 100)
        XCTAssertTrue(sound.isAnnouncementPlaying)
        try await Task.sleep(for: .milliseconds(200))
        XCTAssertEqual(sound.musicPlaybackVolume, 0.162, accuracy: 0.04)
        sound.stopAnnouncement()
        try await Task.sleep(for: .milliseconds(210))
        XCTAssertFalse(sound.isAnnouncementPlaying)
        XCTAssertEqual(sound.musicPlaybackVolume, 0.6, accuracy: 0.04)
        sound.announceCue("correct")
        XCTAssertTrue(sound.isAnnouncementPlaying)
        sound.setActive(false)
        XCTAssertFalse(sound.isAnnouncementPlaying)
        sound.setActive(true)
        XCTAssertTrue(sound.isMusicPlaying)
        XCTAssertFalse(sound.isAnnouncementPlaying)
    }

    @MainActor
    func testRecordedPresenterFinishRestoresMusic() async throws {
        let url = try XCTUnwrap(Bundle.main.url(forResource: "correct", withExtension: "m4a", subdirectory: "Audio/Presenter"))
        let decoder = try AVAudioPlayer(contentsOf: url)
        let sound = GameSound()
        let initial = (sound.announcerEnabled, sound.announcerVolume, sound.musicEnabled, sound.musicVolume)
        defer {
            sound.setActive(false)
            sound.announcerEnabled = initial.0
            sound.announcerVolume = initial.1
            sound.musicEnabled = initial.2
            sound.musicVolume = initial.3
        }
        sound.announcerEnabled = true
        sound.announcerVolume = 0.9
        sound.musicEnabled = true
        sound.musicVolume = 0.6
        sound.music(.stage3)
        sound.announceCue("correct")
        try await Task.sleep(for: .milliseconds(Int(decoder.duration * 1000) + 450))
        XCTAssertFalse(sound.isAnnouncementPlaying)
        XCTAssertEqual(sound.musicPlaybackVolume, 0.6, accuracy: 0.04)
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
