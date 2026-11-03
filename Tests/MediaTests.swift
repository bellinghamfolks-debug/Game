import AVFoundation
import XCTest
@testable import Millionaire

final class MediaTests: XCTestCase {
    @MainActor
    func testBundledScoreAndEffectsOpenOnAppleDecoder() async throws {
        let names = Set(MusicTrack.allCases.map(\.rawValue) + SoundCue.allCases.map(\.file))
        XCTAssertEqual(names.count, 15)
        for name in names.sorted() {
            let url = try XCTUnwrap(Bundle.main.url(forResource: name, withExtension: "m4a", subdirectory: "Audio") ??
                Bundle.main.url(forResource: name, withExtension: "m4a"), name)
            let decoder = try AVAudioPlayer(contentsOf: url)
            XCTAssertGreaterThan(decoder.duration, 0.5, name)
            XCTAssertEqual(decoder.numberOfChannels, 2, name)
            XCTAssertTrue(decoder.prepareToPlay(), name)
        }
    }
    @MainActor
    func testSoundSettingsSurviveControllerRecreation() async {
        let original = GameSound()
        let initial = (original.musicEnabled, original.effectsEnabled, original.musicVolume, original.effectsVolume)
        defer {
            original.musicEnabled = initial.0
            original.effectsEnabled = initial.1
            original.musicVolume = initial.2
            original.effectsVolume = initial.3
        }
        original.musicEnabled.toggle()
        original.effectsEnabled.toggle()
        original.musicVolume = 0.34
        original.effectsVolume = 0.45
        let restored = GameSound()
        XCTAssertEqual(restored.musicEnabled, !initial.0)
        XCTAssertEqual(restored.effectsEnabled, !initial.1)
        XCTAssertEqual(restored.musicVolume, 0.34, accuracy: 0.001)
        XCTAssertEqual(restored.effectsVolume, 0.45, accuracy: 0.001)
    }
}
