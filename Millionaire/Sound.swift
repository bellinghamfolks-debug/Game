import SwiftUI
import AVFoundation
import CoreHaptics

enum MusicTrack: String, CaseIterable { case intro, menu, calm, tense, suspense, victory }
enum SoundCue: String, CaseIterable {
    case question, select, lock, correct, wrong, help, milestone, victory, applause
    var file: String { self == .victory ? "win" : rawValue }
}

@MainActor
final class GameSound: ObservableObject {
    @Published var musicVolume: Double { didSet { defaults.set(musicVolume, forKey: "scoreVolume"); current?.volume = Float(musicVolume) } }
    @Published var effectsVolume: Double { didSet { defaults.set(effectsVolume, forKey: "fxVolume"); effects.values.flatMap { $0 }.forEach { $0.volume = Float(effectsVolume) } } }
    @Published var musicEnabled: Bool { didSet { defaults.set(musicEnabled, forKey: "scoreOn"); synchronize() } }
    @Published var effectsEnabled: Bool { didSet { defaults.set(effectsEnabled, forKey: "fxOn"); if !effectsEnabled { stopEffects() } } }
    private let defaults = UserDefaults.standard
    private var active = true
    private var interrupted = false
    private var target: MusicTrack?
    private var playing: MusicTrack?
    private var current: AVAudioPlayer?
    private var fading: AVAudioPlayer?
    private var effects: [SoundCue: [AVAudioPlayer]] = [:]
    private var haptics: CHHapticEngine?
    private var players: [MusicTrack: AVAudioPlayer] = [:]

    private var observers: [NSObjectProtocol] = []

    init() {
        let d = UserDefaults.standard
        d.register(defaults: ["scoreVolume": 0.62, "fxVolume": 0.85, "scoreOn": true, "fxOn": true])
        musicVolume = d.double(forKey: "scoreVolume")
        effectsVolume = d.double(forKey: "fxVolume")
        musicEnabled = d.bool(forKey: "scoreOn")
        effectsEnabled = d.bool(forKey: "fxOn")
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { [weak self] note in
            let type = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let flags = note.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt
            Task { @MainActor [weak self] in self?.handleInterruption(type: type, options: flags) }
        })
        observers.append(center.addObserver(forName: AVAudioSession.mediaServicesWereResetNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor [weak self] in self?.resetMedia() }
        })
        warmEffects()
    }

    deinit { observers.forEach { NotificationCenter.default.removeObserver($0) } }

    private func warmEffects() {
        for sound in SoundCue.allCases {
            effects[sound] = (0..<(sound == .select ? 2 : 1)).compactMap { _ in player(sound.file) }
        }
    }
    private func player(_ file: String) -> AVAudioPlayer? {
        let bundle = Bundle.main
        guard let url = bundle.url(forResource: file, withExtension: "m4a", subdirectory: "Audio") ??
                bundle.url(forResource: file, withExtension: "m4a") else { return nil }
        guard let result = try? AVAudioPlayer(contentsOf: url) else { return nil }
        result.prepareToPlay()
        return result
    }

    func music(_ track: MusicTrack) { target = track; synchronize() }
    func setActive(_ value: Bool) {
        active = value
        if !value { stopEffects(); haptics?.stop(completionHandler: nil) }
        synchronize()
    }
    private func stopEffects() { effects.values.flatMap { $0 }.forEach { $0.stop(); $0.currentTime = 0 } }

    private func synchronize() {
        guard active, !interrupted, musicEnabled, let track = target else {
            fading?.stop()
            fading = nil
            current?.pause()
            return
        }
        try? AVAudioSession.sharedInstance().setActive(true)
        if track == playing, let current {
            if !current.isPlaying { current.setVolume(Float(musicVolume), fadeDuration: 0); current.play() }
            return
        }
        guard let next = players[track] ?? player(track.rawValue) else { return }
        players[track] = next
        fading?.stop()
        let previous = current
        fading = previous
        previous?.setVolume(0, fadeDuration: 0.7)
        next.numberOfLoops = -1
        next.currentTime = 0
        next.volume = 0
        next.play()
        next.setVolume(Float(musicVolume), fadeDuration: 0.7)
        playing = track
        current = next
        Task { @MainActor [weak self, weak previous] in
            try? await Task.sleep(for: .milliseconds(740))
            guard let self, let previous, self.fading === previous, self.current !== previous else { return }
            previous.stop()
            self.fading = nil
        }
    }

    func cue(_ sound: SoundCue) {
        guard active, !interrupted else { return }
        if effectsEnabled, let pool = effects[sound], let effect = pool.first(where: { !$0.isPlaying }) ?? pool.first {
            try? AVAudioSession.sharedInstance().setActive(true)
            effect.volume = Float(effectsVolume)
            effect.currentTime = 0
            effect.play()
        }
        switch sound {
        case .select, .lock, .correct, .wrong, .milestone, .victory: pulse(sound)
        default: break
        }
    }

    private func pulse(_ sound: SoundCue) {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return }
        do {
            if haptics == nil { haptics = try CHHapticEngine() }
            guard let haptics else { return }
            try haptics.start()
            let beats = (sound == .victory || sound == .milestone) ? 3 : (sound == .wrong ? 2 : 1)
            let events = (0..<beats).map { i in
                CHHapticEvent(eventType: .hapticTransient, parameters: [
                    .init(parameterID: .hapticIntensity, value: sound == .select ? 0.28 : 0.76),
                    .init(parameterID: .hapticSharpness, value: sound == .wrong ? 0.2 : 0.62)
                ], relativeTime: Double(i) * 0.14)
            }
            let pattern = try CHHapticPattern(events: events, parameters: [])
            try haptics.makePlayer(with: pattern).start(atTime: CHHapticTimeImmediate)
        } catch { haptics = nil }
    }

    private func handleInterruption(type: UInt?, options: UInt?) {
        guard let type, let reason = AVAudioSession.InterruptionType(rawValue: type) else { return }
        interrupted = reason == .began
        if interrupted { stopEffects() }
        if reason == .ended, let options, !AVAudioSession.InterruptionOptions(rawValue: options).contains(.shouldResume) {
            return
        }
        synchronize()
    }

    private func resetMedia() {
        current = nil
        fading = nil
        playing = nil
        haptics = nil
        players.removeAll()
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
        warmEffects()
        synchronize()
    }
}
