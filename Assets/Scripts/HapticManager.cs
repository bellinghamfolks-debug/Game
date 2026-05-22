using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Vibration patterns mirroring the Canvas-era haptics: confirm,
    /// warn, danger, stairs, etc. On Android we call Vibrator
    /// directly so the patterns are precise; on other platforms we
    /// use Unity's Handheld.Vibrate as a coarse fallback.
    /// </summary>
    public class HapticManager : MonoBehaviour
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        AndroidJavaObject _vib;
        bool _hasAmplitude;

        void Awake()
        {
            try
            {
                using (var unity = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var ctx = unity.GetStatic<AndroidJavaObject>("currentActivity"))
                {
                    _vib = ctx.Call<AndroidJavaObject>("getSystemService", "vibrator");
                }
                using (var build = new AndroidJavaClass("android.os.Build$VERSION"))
                {
                    _hasAmplitude = build.GetStatic<int>("SDK_INT") >= 26;
                }
            }
            catch { /* silent */ }
        }

        void Pattern(long[] pat)
        {
            if (_vib == null) return;
            try
            {
                if (_hasAmplitude)
                {
                    using (var fx = new AndroidJavaClass("android.os.VibrationEffect"))
                    using (var effect = fx.CallStatic<AndroidJavaObject>("createWaveform", pat, -1))
                        _vib.Call("vibrate", effect);
                }
                else _vib.Call("vibrate", pat, -1);
            }
            catch { }
        }

        public void Tick()    => Pattern(new long[] { 0, 25 });
        public void Confirm() => Pattern(new long[] { 0, 20, 30, 20 });
        public void Warn()    => Pattern(new long[] { 0, 50, 80, 50 });
        public void Danger()  => Pattern(new long[] { 0, 200 });
        public void Stairs()  => Pattern(new long[] { 0, 30, 50, 30, 50, 30 });
        public void Door()    => Pattern(new long[] { 0, 90, 70, 30 });
        public void Person()  => Pattern(new long[] { 0, 40, 40, 40 });
        public void Error()   => Pattern(new long[] { 0, 150, 80, 150 });
#else
        public void Tick()    { Handheld.Vibrate(); }
        public void Confirm() { Handheld.Vibrate(); }
        public void Warn()    { Handheld.Vibrate(); }
        public void Danger()  { Handheld.Vibrate(); }
        public void Stairs()  { Handheld.Vibrate(); }
        public void Door()    { Handheld.Vibrate(); }
        public void Person()  { Handheld.Vibrate(); }
        public void Error()   { Handheld.Vibrate(); }
#endif
    }
}
