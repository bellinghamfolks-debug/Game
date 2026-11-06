import SwiftUI
import AVFoundation
import CoreHaptics

enum MusicTrack: String, CaseIterable {
    case intro, menu, stage1, stage2, stage3, stage4, stage5, suspense, victory

    static func station(questionNumber: Int) -> MusicTrack {
        switch questionNumber {
        case ...3: return .stage1
        case 4...6: return .stage2
        case 7...9: return .stage3
        case 10...12: return .stage4
        default: return .stage5
        }
    }

    var isStation: Bool {
        switch self {
        case .stage1, .stage2, .stage3, .stage4, .stage5: return true
        default: return false
        }
    }

    var transitionLength: TimeInterval {
        switch self {
        case .suspense: return 0.16
        case .victory, .intro: return 0.28
        default: return 0.70
        }
    }
}
enum SoundCue: String, CaseIterable {
    case question, select, lock, correct, wrong, help, milestone, victory, applause, countdown
    var file: String { self == .victory ? "win" : rawValue }
}

private final class PresenterCompletion: NSObject, AVAudioPlayerDelegate {
    var completed: (@MainActor (ObjectIdentifier) -> Void)?

    private func finish(_ player: AVAudioPlayer) {
        let id = ObjectIdentifier(player)
        Task { @MainActor [weak self] in self?.completed?(id) }
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) { finish(player) }
    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) { finish(player) }
}

enum PresenterAudio {
    static let cues = ["correct", "wrong", "safety-1", "safety-2", "win", "timeout", "withdraw", "question-generic"]
    static let files = (1...15).map { "question-" + String($0) } + cues

    static func question(number: Int, prize: Int?) -> String {
        guard prize != nil, (1...15).contains(number) else { return "question-generic" }
        return "question-" + String(number)
    }
}

@MainActor
final class GameSound: ObservableObject {
    @Published var musicVolume: Double { didSet { defaults.set(musicVolume, forKey: "scoreVolume"); adjustMusic(fade: 0.08) } }
    @Published var effectsVolume: Double { didSet { defaults.set(effectsVolume, forKey: "fxVolume"); effects.values.flatMap { $0 }.forEach { $0.volume = Float(effectsVolume) } } }
    @Published var musicEnabled: Bool { didSet { defaults.set(musicEnabled, forKey: "scoreOn"); synchronize() } }
    @Published var effectsEnabled: Bool { didSet { defaults.set(effectsEnabled, forKey: "fxOn"); if !effectsEnabled { stopEffects() } } }
    @Published var announcerEnabled: Bool {
        didSet {
            defaults.set(announcerEnabled, forKey: "announcerOn")
            if !announcerEnabled { stopAnnouncement() }
        }
    }
    @Published var announcerVolume: Double {
        didSet {
            defaults.set(announcerVolume, forKey: "announcerVolume")
            if announcerVolume <= 0 { stopAnnouncement() }
            else { presenter?.volume = Float(announcerVolume) }
        }
    }
    private let presenterCompletion = PresenterCompletion()
    private var presenter: AVAudioPlayer?
    var isAnnouncementPlaying: Bool { presenter?.isPlaying == true }
    var musicPlaybackVolume: Float { current?.volume ?? 0 }
    private var recovery: Task<Void, Never>?
    var isMusicPlaying: Bool { current?.isPlaying == true }
    private var musicGain: Float { Float(musicVolume) * (presenter == nil ? 1.0 : 0.27) }
    private let defaults = UserDefaults.standard
    private var active = true
    private var interrupted = false
    private var target: MusicTrack?
    private var playing: MusicTrack?
    private var suspendedBed: (track: MusicTrack, position: TimeInterval)?
    private var fadeCleanup: Task<Void, Never>?
    private var current: AVAudioPlayer?
    private var fading: AVAudioPlayer?
    private var effects: [SoundCue: [AVAudioPlayer]] = [:]
    private var haptics: CHHapticEngine?
    private var players: [MusicTrack: AVAudioPlayer] = [:]

    private var observers: [NSObjectProtocol] = []

    init() {
        let d = UserDefaults.standard
        d.register(defaults: ["scoreVolume": 0.62, "fxVolume": 0.85, "scoreOn": true, "fxOn": true, "announcerOn": true, "announcerVolume": 0.95])
        musicVolume = d.double(forKey: "scoreVolume")
        effectsVolume = d.double(forKey: "fxVolume")
        musicEnabled = d.bool(forKey: "scoreOn")
        effectsEnabled = d.bool(forKey: "fxOn")
        announcerEnabled = d.bool(forKey: "announcerOn")
        announcerVolume = d.double(forKey: "announcerVolume")
        configurePresenter()
        configureSession()
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { [weak self] note in
            let type = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let flags = note.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt
            let suspended = (note.userInfo?[AVAudioSessionInterruptionWasSuspendedKey] as? NSNumber)?.boolValue == true
            Task { @MainActor [weak self] in self?.handleInterruption(type: type, options: flags, wasSuspended: suspended) }
        })
        observers.append(center.addObserver(forName: AVAudioSession.mediaServicesWereResetNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor [weak self] in self?.resetMedia() }
        })
        observers.append(center.addObserver(forName: AVAudioSession.routeChangeNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor [weak self] in self?.scheduleRecovery() }
        })
        warmEffects()
    }

    private func configureSession() {
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
    }

    private func configurePresenter() {
        presenterCompletion.completed = { [weak self] id in
            guard let self, let presenter = self.presenter, ObjectIdentifier(presenter) == id else { return }
            self.presenter = nil
            self.adjustMusic(fade: 0.24)
        }
    }

    private func adjustMusic(fade: TimeInterval) {
        current?.setVolume(musicGain, fadeDuration: fade)
    }

    func announceQuestion(number: Int, prize: Int?) {
        announceCue(PresenterAudio.question(number: number, prize: prize))
    }

    func announceCue(_ cue: String) {
        guard active, !interrupted, announcerEnabled, announcerVolume > 0,
              PresenterAudio.files.contains(cue),
              let line = player(cue, subdirectory: "Audio/Presenter") else { return }
        stopAnnouncement()
        do { try AVAudioSession.sharedInstance().setActive(true) } catch { return }
        line.delegate = presenterCompletion
        line.volume = Float(announcerVolume)
        presenter = line
        adjustMusic(fade: 0.12)
        if !line.play() { stopAnnouncement() }
    }

    func stopAnnouncement() {
        presenter?.delegate = nil
        presenter?.stop()
        presenter = nil
        adjustMusic(fade: 0.15)
    }

    deinit { observers.forEach { NotificationCenter.default.removeObserver($0) } }

    private func warmEffects() {
        for sound in SoundCue.allCases {
            effects[sound] = (0..<(sound == .select ? 2 : 1)).compactMap { _ in player(sound.file) }
        }
    }
    private func player(_ file: String, subdirectory: String = "Audio") -> AVAudioPlayer? {
        let bundle = Bundle.main
        let flattened = subdirectory == "Audio" ? bundle.url(forResource: file, withExtension: "m4a") : nil
        guard let url = bundle.url(forResource: file, withExtension: "m4a", subdirectory: subdirectory) ?? flattened else { return nil }
        guard let result = try? AVAudioPlayer(contentsOf: url) else { return nil }
        result.prepareToPlay()
        return result
    }

    func music(_ track: MusicTrack) { target = track; synchronize() }
    func setActive(_ value: Bool) {
        active = value
        recovery?.cancel()
        if !value {
            stopAnnouncement()
            stopEffects()
            haptics?.stop(completionHandler: nil)
            synchronize()
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            return
        }
        // iOS may suspend the process before delivering an interruption-ended notification.
        interrupted = false
        configureSession()
        synchronize()
        scheduleRecovery()
    }

    private func scheduleRecovery() {
        guard active, !interrupted, musicEnabled, target != nil else { return }
        recovery?.cancel()
        recovery = Task { @MainActor [weak self] in
            for delay in [100, 350, 900, 1800] {
                try? await Task.sleep(for: .milliseconds(delay))
                guard !Task.isCancelled, let self, self.active, !self.interrupted,
                      self.musicEnabled else { return }
                if self.current?.isPlaying == true, self.playing == self.target { return }
                self.synchronize()
            }
        }
    }

    private func stopEffects() { effects.values.flatMap { $0 }.forEach { $0.stop(); $0.currentTime = 0 } }

    private func synchronize() {
        guard active, !interrupted, musicEnabled, let track = target else {
            fading?.stop()
            fading = nil
            current?.pause()
            return
        }
        do { try AVAudioSession.sharedInstance().setActive(true) } catch { return }
        if track == playing, let current {
            if !current.isPlaying { current.setVolume(musicGain, fadeDuration: 0); current.play() }
            return
        }
        guard let next = players[track] ?? player(track.rawValue) else { return }
        players[track] = next
        fadeCleanup?.cancel()
        fading?.stop()
        let previous = current
        if let playing, playing.isStation, track == .suspense, let previous {
            suspendedBed = (playing, previous.currentTime)
        }
        let resumeAt = suspendedBed?.track == track ? suspendedBed?.position : nil
        if track != .suspense { suspendedBed = nil }
        let fade = track.transitionLength
        fading = previous
        previous?.setVolume(0, fadeDuration: fade)
        next.numberOfLoops = -1
        next.currentTime = resumeAt ?? 0
        next.volume = previous == nil ? musicGain : 0
        next.play()
        next.setVolume(musicGain, fadeDuration: fade)
        playing = track
        current = next
        fadeCleanup = Task { @MainActor [weak self, weak previous] in
            try? await Task.sleep(for: .milliseconds(Int(fade * 1000) + 40))
            guard !Task.isCancelled, let self, let previous,
                  self.fading === previous, self.current !== previous else { return }
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

    private func handleInterruption(type: UInt?, options: UInt?, wasSuspended: Bool) {
        guard let type, let reason = AVAudioSession.InterruptionType(rawValue: type) else { return }
        // A suspension notification describes a past deactivation, not an ongoing call.
        if reason == .began, wasSuspended {
            if active {
                interrupted = false
                synchronize()
                scheduleRecovery()
            }
            return
        }
        interrupted = reason == .began
        if interrupted { stopAnnouncement(); stopEffects(); recovery?.cancel() }
        if reason == .ended, let options, !AVAudioSession.InterruptionOptions(rawValue: options).contains(.shouldResume) {
            return
        }
        synchronize()
        if !interrupted { scheduleRecovery() }
    }

    private func resetMedia() {
        fadeCleanup?.cancel()
        suspendedBed = nil
        current = nil
        fading = nil
        playing = nil
        haptics = nil
        players.removeAll()
        interrupted = false
        stopAnnouncement()
        configurePresenter()
        configureSession()
        warmEffects()
        synchronize()
        scheduleRecovery()
    }
}
