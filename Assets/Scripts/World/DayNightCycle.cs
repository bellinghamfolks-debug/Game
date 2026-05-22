using UnityEngine;

namespace BlindLife.World
{
    /// <summary>
    /// Drives the sun rotation, sky tint and fog based on the in-game
    /// clock. Day length = 60 game-minutes per dawn-to-dusk window.
    /// </summary>
    [RequireComponent(typeof(Light))]
    public class DayNightCycle : MonoBehaviour
    {
        Light _sun;
        public Gradient ambientGradient;
        public Gradient fogGradient;
        public Gradient sunGradient;
        public AnimationCurve sunIntensity = AnimationCurve.EaseInOut(0, 0.1f, 1, 1.2f);

        void Awake()
        {
            _sun = GetComponent<Light>();
            if (ambientGradient == null) ambientGradient = BuildDefaultAmbient();
            if (fogGradient == null) fogGradient = BuildDefaultFog();
            if (sunGradient == null) sunGradient = BuildDefaultSun();
        }

        void Update()
        {
            var gm = GameManager.Instance;
            if (gm == null) return;
            // 0..1 across a 1080-minute day (06:00 → 24:00).
            float phase = Mathf.Clamp01(gm.State.minutes / 1080f);
            // Sun arcs across the sky from -10° (dawn) to 200° (dusk),
            // dipping past the horizon at night.
            float sunAngle = Mathf.Lerp(-10f, 200f, phase);
            transform.rotation = Quaternion.Euler(sunAngle, -30f, 0);
            _sun.color = sunGradient.Evaluate(phase);
            float dot = Mathf.Clamp01(Vector3.Dot(Vector3.up, -transform.forward));
            _sun.intensity = sunIntensity.Evaluate(phase) * Mathf.Max(0.1f, dot);
            RenderSettings.ambientLight = ambientGradient.Evaluate(phase);
            RenderSettings.fogColor = fogGradient.Evaluate(phase);
        }

        static Gradient BuildDefaultAmbient()
        {
            var g = new Gradient();
            g.SetKeys(new[] {
                new GradientColorKey(new Color(0.20f, 0.22f, 0.32f), 0.00f), // dawn
                new GradientColorKey(new Color(0.62f, 0.62f, 0.65f), 0.30f), // morning
                new GradientColorKey(new Color(0.70f, 0.70f, 0.72f), 0.55f), // noon
                new GradientColorKey(new Color(0.65f, 0.50f, 0.35f), 0.75f), // golden
                new GradientColorKey(new Color(0.30f, 0.22f, 0.28f), 0.90f), // dusk
                new GradientColorKey(new Color(0.10f, 0.12f, 0.18f), 1.00f)  // night
            }, new[] {
                new GradientAlphaKey(1, 0), new GradientAlphaKey(1, 1)
            });
            return g;
        }
        static Gradient BuildDefaultFog()
        {
            var g = new Gradient();
            g.SetKeys(new[] {
                new GradientColorKey(new Color(0.35f, 0.40f, 0.50f), 0.00f),
                new GradientColorKey(new Color(0.78f, 0.84f, 0.90f), 0.30f),
                new GradientColorKey(new Color(0.85f, 0.88f, 0.92f), 0.55f),
                new GradientColorKey(new Color(0.78f, 0.60f, 0.45f), 0.78f),
                new GradientColorKey(new Color(0.45f, 0.30f, 0.30f), 0.90f),
                new GradientColorKey(new Color(0.10f, 0.12f, 0.20f), 1.00f),
            }, new[] {
                new GradientAlphaKey(1, 0), new GradientAlphaKey(1, 1)
            });
            return g;
        }
        static Gradient BuildDefaultSun()
        {
            var g = new Gradient();
            g.SetKeys(new[] {
                new GradientColorKey(new Color(1.00f, 0.55f, 0.30f), 0.00f),
                new GradientColorKey(new Color(1.00f, 0.92f, 0.78f), 0.30f),
                new GradientColorKey(new Color(1.00f, 0.98f, 0.92f), 0.55f),
                new GradientColorKey(new Color(1.00f, 0.78f, 0.50f), 0.78f),
                new GradientColorKey(new Color(0.80f, 0.40f, 0.30f), 0.90f),
                new GradientColorKey(new Color(0.15f, 0.20f, 0.40f), 1.00f),
            }, new[] {
                new GradientAlphaKey(1, 0), new GradientAlphaKey(1, 1)
            });
            return g;
        }
    }
}
