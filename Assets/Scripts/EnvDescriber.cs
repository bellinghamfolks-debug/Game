using System.Collections.Generic;
using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Generates short Arabic descriptions of what surrounds the player
    /// using world-space queries. No AI required.
    /// </summary>
    public static class EnvDescriber
    {
        static readonly Collider[] _buf = new Collider[24];

        public static string Describe(Transform player)
        {
            if (player == null) return "";
            int n = Physics.OverlapSphereNonAlloc(player.position, 10f, _buf);
            var near = new List<(Interactable e, float d, string side)>();
            foreach (var col in _buf)
            {
                if (col == null) continue;
                var it = col.GetComponentInParent<Interactable>();
                if (it == null) continue;
                float d = Vector3.Distance(player.position, col.transform.position);
                if (d > 9f) continue;
                near.Add((it, d, RelativeSide(player, col.transform.position)));
            }
            near.Sort((a, b) => a.d.CompareTo(b.d));

            var sb = new System.Text.StringBuilder();
            if (near.Count == 0) sb.Append("لا شيء قريب منك مباشرة. المكان هادئ.");
            else
            {
                int count = Mathf.Min(4, near.Count);
                for (int i = 0; i < count; i++)
                {
                    var (e, d, side) = near[i];
                    string steps = StepsArabic(d);
                    sb.Append(e.displayName).Append(' ').Append(side)
                      .Append(" يبعد ").Append(steps);
                    if (!string.IsNullOrEmpty(e.description))
                        sb.Append(" — ").Append(e.description);
                    sb.Append(". ");
                }
                if (near.Count > count) sb.Append("هناك أيضًا ").Append(near.Count - count).Append(" عناصر أبعد.");
            }
            return sb.ToString().Trim();
        }

        static string RelativeSide(Transform player, Vector3 worldPos)
        {
            Vector3 v = worldPos - player.position;
            v.y = 0;
            float fwd = Vector3.Dot(v.normalized, player.forward);
            float right = Vector3.Dot(v.normalized, player.right);
            if (fwd > 0.7f) return "أمامك";
            if (fwd > 0.3f && right > 0.3f) return "أمامك على اليمين";
            if (fwd > 0.3f && right < -0.3f) return "أمامك على اليسار";
            if (Mathf.Abs(fwd) <= 0.3f && right > 0.3f) return "على يمينك";
            if (Mathf.Abs(fwd) <= 0.3f && right < -0.3f) return "على يسارك";
            if (fwd < -0.3f && right > 0.3f) return "خلفك على اليمين";
            if (fwd < -0.3f && right < -0.3f) return "خلفك على اليسار";
            return "خلفك";
        }

        static string StepsArabic(float metres)
        {
            // ~0.7m per step
            int n = Mathf.Max(1, Mathf.RoundToInt(metres / 0.7f));
            switch (n)
            {
                case 1: return "خطوة واحدة";
                case 2: return "خطوتين";
                case 3: return "ثلاث خطوات";
                case 4: return "أربع خطوات";
                case 5: return "خمس خطوات";
                case 6: return "ست خطوات";
                case 7: return "سبع خطوات";
                case 8: return "ثماني خطوات";
                case 9: return "تسع خطوات";
                case 10: return "عشر خطوات";
                default: return "حوالي " + n + " خطوة";
            }
        }
    }
}
