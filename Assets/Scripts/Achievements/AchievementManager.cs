using System.Collections.Generic;
using System.Text;
using UnityEngine;

namespace BlindLife.Achievements
{
    /// <summary>
    /// Tracks counters + unlocked set, fires AccessibilityManager
    /// announcement on unlock, persists via PlayerPrefs.
    /// </summary>
    public class AchievementManager
    {
        readonly Dictionary<string, Achievement> _all = new Dictionary<string, Achievement>();
        readonly Dictionary<string, int> _counters = new Dictionary<string, int>();
        readonly HashSet<string> _distinct = new HashSet<string>();
        const string PrefKey = "blindlife_achievements_v1";

        public AchievementManager()
        {
            foreach (var a in Achievements.Defaults()) _all[a.id] = a;
        }

        public IEnumerable<Achievement> All => _all.Values;
        public int UnlockedCount
        {
            get { int n = 0; foreach (var a in _all.Values) if (a.unlocked) n++; return n; }
        }
        public int Total => _all.Count;

        public void Unlock(string id)
        {
            if (!_all.TryGetValue(id, out var a)) return;
            if (a.unlocked) return;
            a.unlocked = true;
            AccessibilityManager.Instance?.Speak("إنجاز مفتوح: " + a.title);
            Save();
        }

        public void Bump(string counterId, int threshold, string achievementId)
        {
            _counters.TryGetValue(counterId, out int n);
            n++;
            _counters[counterId] = n;
            if (n >= threshold) Unlock(achievementId);
        }

        public void RecordDistinct(string setId, string member, int minSize, string achievementId)
        {
            if (string.IsNullOrEmpty(member)) return;
            string composite = setId + ":" + member;
            if (!_distinct.Add(composite)) return;
            // count distinct entries for this setId
            int count = 0;
            string prefix = setId + ":";
            foreach (var k in _distinct) if (k.StartsWith(prefix)) count++;
            if (count >= minSize) Unlock(achievementId);
        }

        public string Render()
        {
            var sb = new StringBuilder();
            sb.Append("الإنجازات (").Append(UnlockedCount).Append("/").Append(Total).Append("):\n\n");
            foreach (var a in _all.Values)
            {
                sb.Append(a.unlocked ? "✓ " : "○ ").Append(a.title).Append('\n');
                sb.Append("   ").Append(a.description).Append('\n');
            }
            return sb.ToString();
        }

        public void Save()
        {
            var sb = new StringBuilder();
            foreach (var a in _all.Values) if (a.unlocked) sb.Append(a.id).Append(';');
            PlayerPrefs.SetString(PrefKey, sb.ToString());
            PlayerPrefs.Save();
        }

        public void Load()
        {
            string raw = PlayerPrefs.GetString(PrefKey, "");
            if (string.IsNullOrEmpty(raw)) return;
            foreach (var id in raw.Split(';'))
                if (_all.TryGetValue(id, out var a)) a.unlocked = true;
        }
    }
}
