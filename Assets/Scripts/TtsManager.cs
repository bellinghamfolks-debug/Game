using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Cross-platform TTS. On Android we call into android.speech.tts.TextToSpeech
    /// via AndroidJavaObject so we get the system Arabic voice for free. On
    /// the editor / other platforms it falls back to Debug.Log so calls are safe.
    /// </summary>
    public class TtsManager : MonoBehaviour
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        AndroidJavaObject _tts;
        AndroidJavaObject _activity;
        bool _ready;

        void Start()
        {
            try
            {
                using (var unity = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                {
                    _activity = unity.GetStatic<AndroidJavaObject>("currentActivity");
                }
                var listener = new TtsInitListener(this);
                _tts = new AndroidJavaObject("android.speech.tts.TextToSpeech", _activity, listener);
            }
            catch (System.Exception e) { Debug.LogWarning("TTS init failed: " + e); }
        }

        public void OnInitDone(int status)
        {
            // status==0 means SUCCESS in android.speech.tts.TextToSpeech.SUCCESS
            if (status != 0) { Debug.LogWarning("TTS init status: " + status); return; }
            try
            {
                using (var localeAr = new AndroidJavaObject("java.util.Locale", "ar"))
                {
                    int r = _tts.Call<int>("setLanguage", localeAr);
                    if (r == -1 || r == -2)
                    {
                        using (var localeEn = new AndroidJavaObject("java.util.Locale", "en"))
                            _tts.Call<int>("setLanguage", localeEn);
                    }
                }
                _ready = true;
            }
            catch (System.Exception e) { Debug.LogWarning("TTS setLanguage: " + e); }
        }

        public void Speak(string text)
        {
            if (!_ready || _tts == null || string.IsNullOrEmpty(text)) return;
            try { _tts.Call<int>("speak", text, 1 /*QUEUE_ADD*/, null, System.Guid.NewGuid().ToString()); }
            catch (System.Exception e) { Debug.LogWarning("TTS speak: " + e); }
        }
        public void SpeakNow(string text)
        {
            if (!_ready || _tts == null || string.IsNullOrEmpty(text)) return;
            try { _tts.Call<int>("speak", text, 0 /*QUEUE_FLUSH*/, null, System.Guid.NewGuid().ToString()); }
            catch (System.Exception e) { Debug.LogWarning("TTS speakNow: " + e); }
        }
        public void Stop()
        {
            if (_tts == null) return;
            try { _tts.Call<int>("stop"); } catch { }
        }
        void OnDestroy()
        {
            if (_tts == null) return;
            try { _tts.Call<int>("stop"); _tts.Call("shutdown"); } catch { }
        }

        class TtsInitListener : AndroidJavaProxy
        {
            readonly TtsManager _owner;
            public TtsInitListener(TtsManager o) : base("android.speech.tts.TextToSpeech$OnInitListener") { _owner = o; }
            public void onInit(int status) { _owner.OnInitDone(status); }
        }
#else
        public void Speak(string text)    { Debug.Log("[TTS] " + text); }
        public void SpeakNow(string text) { Debug.Log("[TTS!] " + text); }
        public void Stop() {}
#endif
    }
}
