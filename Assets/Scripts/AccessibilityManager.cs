using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Single entry point for spoken text + haptics + audio cues.
    /// Wraps <see cref="TtsManager"/> and <see cref="HapticManager"/>
    /// so callers express intent (confirm, warn, danger) instead of
    /// coordinating both subsystems.
    /// </summary>
    public class AccessibilityManager : MonoBehaviour
    {
        public static AccessibilityManager Instance { get; private set; }

        TtsManager _tts;
        HapticManager _haptics;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(this); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            _tts = gameObject.AddComponent<TtsManager>();
            _haptics = gameObject.AddComponent<HapticManager>();
        }

        public void Speak(string text)    => _tts?.Speak(text);
        public void SpeakNow(string text) => _tts?.SpeakNow(text);
        public void Stop()                => _tts?.Stop();

        public void Confirm() => _haptics?.Confirm();
        public void Warn(string text = null)
        {
            _haptics?.Warn();
            if (!string.IsNullOrEmpty(text)) _tts?.SpeakNow(text);
        }
        public void Danger(string text = null)
        {
            _haptics?.Danger();
            if (!string.IsNullOrEmpty(text)) _tts?.SpeakNow(text);
        }
    }
}
