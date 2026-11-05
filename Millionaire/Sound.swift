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

private final class SpeechCompletion: NSObject, AVSpeechSynthesizerDelegate {
    var completed: (@MainActor (ObjectIdentifier) -> Void)?

    private func finish(_ utterance: AVSpeechUtterance) {
        let id = ObjectIdentifier(utterance)
        Task { @MainActor [weak self] in self?.completed?(id) }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) { finish(utterance) }
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) { finish(utterance) }
}

struct AnnouncerScript {
    private static let ordinals = ["الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع",
                                   "الثامن", "التاسع", "العاشر", "الحادي عشر", "الثاني عشر", "الثالث عشر",
                                   "الرابع عشر", "الخامس عشر"]

    static func question(number: Int, prize: Int?) -> String {
        guard number > 0 else { return "" }
        let position = number <= ordinals.count ? ordinals[number - 1] : "رقم " + spelled(number)
        guard let prize, prize > 0 else { return "السؤال " + position + "." }
        let opening = number == 15 ? "نصل إلى سؤال المليون! " : ""
        let value = prize == 1_000_000 ? "مليون" : spelled(prize)
        return opening + "السؤال " + position + "، على " + value + " ريال."
    }

    private static func spelled(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "ar_SA")
        formatter.numberStyle = .spellOut
        return formatter.string(from: NSNumber(value: value)) ?? String(value)
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
        }
    }
    private var speech = AVSpeechSynthesizer()
    private let speechCompletion = SpeechCompletion()
    private var spoken: AVSpeechUtterance?
    private lazy var arabicVoice: AVSpeechSynthesisVoice? = {
        let installed = AVSpeechSynthesisVoice.speechVoices().filter { $0.language.lowercased().hasPrefix("ar") }
        return installed.sorted {
            if $0.quality != $1.quality { return $0.quality.rawValue > $1.quality.rawValue }
            return $0.language.lowercased() == "ar-sa" && $1.language.lowercased() != "ar-sa"
        }.first ?? AVSpeechSynthesisVoice(language: "ar-SA")
    }()
    private var recovery: Task<Void, Never>?
    var isMusicPlaying: Bool { current?.isPlaying == true }
    private var musicGain: Float { Float(musicVolume) * (spoken == nil ? 1.0 : 0.27) }
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
        configureSpeech()
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

    private func configureSpeech() {
        speech.usesApplicationAudioSession = true
        speech.delegate = speechCompletion
        speechCompletion.completed = { [weak self] id in
            guard let self, let spoken = self.spoken, ObjectIdentifier(spoken) == id else { return }
            self.spoken = nil
            self.adjustMusic(fade: 0.24)
        }
    }

    private func adjustMusic(fade: TimeInterval) {
        current?.setVolume(musicGain, fadeDuration: fade)
    }

    func announceQuestion(number: Int, prize: Int?) {
        announce(AnnouncerScript.question(number: number, prize: prize))
    }

    func announce(_ text: String) {
        guard active, !interrupted, announcerEnabled, announcerVolume > 0,
              !text.isEmpty, let voice = arabicVoice else { return }
        stopAnnouncement()
        do { try AVAudioSession.sharedInstance().setActive(true) } catch { return }
        let line = AVSpeechUtterance(string: text)
        line.voice = voice
        line.rate = AVSpeechUtteranceDefaultSpeechRate * 0.94
        line.pitchMultiplier = 0.96
        line.volume = Float(announcerVolume)
        spoken = line
        adjustMusic(fade: 0.12)
        speech.speak(line)
    }

    func stopAnnouncement() {
        spoken = nil
        if speech.isSpeaking || speech.isPaused { speech.stopSpeaking(at: .immediate) }
        adjustMusic(fade: 0.15)
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
        spoken = nil
        speech.delegate = nil
        speech = AVSpeechSynthesizer()
        configureSpeech()
        configureSession()
        warmEffects()
        synchronize()
        scheduleRecovery()
    }
}
