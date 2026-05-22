using UnityEngine;

namespace BlindLife.Hazards
{
    /// <summary>
    /// Active hazards with a reaction window:
    ///   1. Game announces a warning ('دراجة قادمة من يمينك! توقف').
    ///   2. Player has ~1.6 s to stop moving.
    ///   3. If still moving when the window closes → impact (stat loss).
    ///   4. Otherwise → near-miss (+ confidence).
    /// </summary>
    public class HazardSystem : MonoBehaviour
    {
        public enum Kind { Bicycle, Car, OpenHole }

        public delegate void WarningHandler(string text, string shortLabel, float windowSeconds);
        public delegate void ResolveHandler(string text, bool impact);
        public event WarningHandler OnWarning;
        public event ResolveHandler OnResolve;

        [Header("Tuning")]
        public float windowSeconds = 1.6f;
        public float cooldownSeconds = 14f;
        public float frequencyPerSec = 0.05f;

        bool _armed;
        float _warnedAt;
        Kind _kind;
        float _lastResolvedAt;

        public bool IsArmed => _armed;

        public void SetDifficulty(int level)
        {
            switch (level)
            {
                case 0: windowSeconds = 2.5f; cooldownSeconds = 20f; frequencyPerSec = 0.025f; break;
                case 2: windowSeconds = 1.2f; cooldownSeconds = 9f;  frequencyPerSec = 0.085f; break;
                default: windowSeconds = 1.6f; cooldownSeconds = 14f; frequencyPerSec = 0.05f; break;
            }
        }

        public void Tick(bool playerMoving, GameState gs)
        {
            if (_armed)
            {
                if (Time.time - _warnedAt >= windowSeconds)
                {
                    bool impact = playerMoving;
                    if (impact) ApplyImpact(gs, _kind);
                    else ApplyNearMiss(gs);
                    string text = ResolveText(_kind, impact);
                    _armed = false;
                    _lastResolvedAt = Time.time;
                    OnResolve?.Invoke(text, impact);
                }
                return;
            }
            if (Time.time - _lastResolvedAt < cooldownSeconds) return;
            // Per-frame chance (frequency is per second, multiplied by dt).
            if (Random.value > frequencyPerSec * Time.deltaTime) return;
            _armed = true;
            _warnedAt = Time.time;
            _kind = (Random.value < 0.55f) ? Kind.Bicycle : Kind.Car;
            OnWarning?.Invoke(WarningText(_kind), ShortLabel(_kind), windowSeconds);
        }

        public void Reset()
        {
            _armed = false;
            _lastResolvedAt = 0f;
        }

        string WarningText(Kind k) => k switch
        {
            Kind.Bicycle => "تنبيه! دراجة قادمة من يمينك. توقف الآن.",
            Kind.Car     => "تنبيه! سيارة قريبة. ثبّت قدميك.",
            _            => "تنبيه! عائق أمامك."
        };

        string ShortLabel(Kind k) => k switch
        {
            Kind.Bicycle => "دراجة",
            Kind.Car     => "سيارة",
            _            => "خطر"
        };

        string ResolveText(Kind k, bool impact)
        {
            if (impact)
                return k switch
                {
                    Kind.Bicycle => "اصطدمت بالدراجة. خفض مهارة الحركة قليلًا.",
                    Kind.Car     => "خدشتك السيارة. كن أكثر حذرًا.",
                    _            => "تعثّرت قليلًا."
                };
            return k switch
            {
                Kind.Bicycle => "أحسنت! الدراجة عبرت بأمان.",
                Kind.Car     => "أحسنت! السيارة ابتعدت.",
                _            => "تجنّبتها بأمان."
            };
        }

        void ApplyImpact(GameState gs, Kind k)
        {
            switch (k)
            {
                case Kind.Bicycle: gs.mobility -= 3; gs.confidence -= 2; break;
                case Kind.Car:     gs.mobility -= 5; gs.confidence -= 4; break;
                default:           gs.mobility -= 1; break;
            }
            gs.ClampStats();
        }

        void ApplyNearMiss(GameState gs)
        {
            gs.mobility += 1;
            gs.confidence += 1;
            gs.ClampStats();
        }
    }
}
