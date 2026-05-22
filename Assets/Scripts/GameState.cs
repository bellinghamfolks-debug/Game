using System.Collections.Generic;
using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// All persistent + transient gameplay state in one place so missions,
    /// AI and the UI all read from the same source.
    /// </summary>
    public class GameState
    {
        public int day = 1;
        public int minutes;
        public int missionIdx;
        public int mobility = 30, social = 30, tech = 20, confidence = 25;
        public Transform playerTransform;

        public readonly HashSet<string> flags = new HashSet<string>();

        public bool HasFlag(string f) => flags.Contains(f);
        public void SetFlag(string f) { if (!string.IsNullOrEmpty(f)) flags.Add(f); }
        public void ClearFlag(string f) { if (!string.IsNullOrEmpty(f)) flags.Remove(f); }
        public void ClampStats()
        {
            mobility    = Mathf.Clamp(mobility, 0, 100);
            social      = Mathf.Clamp(social, 0, 100);
            tech        = Mathf.Clamp(tech, 0, 100);
            confidence  = Mathf.Clamp(confidence, 0, 100);
        }

        public string FormatTime()
        {
            int total = 6 * 60 + minutes;
            int hh = (total / 60) % 24;
            int mm = total % 60;
            return $"{hh:D2}:{mm:D2}";
        }
    }
}
