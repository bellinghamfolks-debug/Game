using System.Collections.Generic;
using System.Text;
using UnityEngine;

namespace BlindLife.AI
{
    /// <summary>
    /// Per-NPC durable record of how the player has behaved. Fed back
    /// into AiDialogueManager so the LLM knows your history with each
    /// character.
    /// </summary>
    public class CharacterMemory
    {
        public class Entry
        {
            public int interactions;
            public int kindness;      // -10..+10
            public int helpAccepted;
            public int helpRefused;
            public long lastSeenMs;
        }

        readonly Dictionary<string, Entry> _entries = new Dictionary<string, Entry>();
        const string PrefKey = "blindlife_charmemory_v1";

        public Entry Get(string npcId)
        {
            if (string.IsNullOrEmpty(npcId)) return new Entry();
            if (!_entries.TryGetValue(npcId, out var e))
            {
                e = new Entry();
                _entries[npcId] = e;
            }
            return e;
        }

        public void RecordInteraction(string npcId)
        {
            if (string.IsNullOrEmpty(npcId)) return;
            var e = Get(npcId);
            e.interactions++;
            e.lastSeenMs = System.DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        }

        public void RecordKindness(string npcId, int delta)
        {
            if (string.IsNullOrEmpty(npcId)) return;
            var e = Get(npcId);
            e.kindness = Mathf.Clamp(e.kindness + delta, -10, 10);
        }

        public void RecordHelp(string npcId, bool accepted)
        {
            if (string.IsNullOrEmpty(npcId)) return;
            var e = Get(npcId);
            if (accepted) e.helpAccepted++; else e.helpRefused++;
        }

        public string DescribeRelationship(string npcId)
        {
            if (!_entries.TryGetValue(npcId, out var e) || e.interactions == 0) return "أول لقاء";
            if (e.kindness >= 5) return "علاقة دافئة، قابلتها " + e.interactions + " مرة";
            if (e.kindness <= -5) return "علاقة متوترة، قابلتها " + e.interactions + " مرة";
            if (e.helpAccepted > e.helpRefused) return "تعتمد عليها أحيانًا";
            if (e.helpRefused > e.helpAccepted) return "تفضّل ألا تطلب منها مساعدة";
            if (e.interactions == 1) return "تقابلتما مرة واحدة";
            return "علاقة عادية";
        }

        public void Clear() => _entries.Clear();

        public void Save()
        {
            var sb = new StringBuilder();
            foreach (var kv in _entries)
            {
                sb.Append(kv.Key).Append(',')
                  .Append(kv.Value.interactions).Append(',')
                  .Append(kv.Value.kindness).Append(',')
                  .Append(kv.Value.helpAccepted).Append(',')
                  .Append(kv.Value.helpRefused).Append(',')
                  .Append(kv.Value.lastSeenMs).Append(';');
            }
            PlayerPrefs.SetString(PrefKey, sb.ToString());
            PlayerPrefs.Save();
        }

        public void Load()
        {
            _entries.Clear();
            string raw = PlayerPrefs.GetString(PrefKey, "");
            if (string.IsNullOrEmpty(raw)) return;
            foreach (var record in raw.Split(';'))
            {
                if (string.IsNullOrWhiteSpace(record)) continue;
                var parts = record.Split(',');
                if (parts.Length < 6) continue;
                var e = new Entry();
                if (int.TryParse(parts[1], out var v1)) e.interactions = v1;
                if (int.TryParse(parts[2], out var v2)) e.kindness = v2;
                if (int.TryParse(parts[3], out var v3)) e.helpAccepted = v3;
                if (int.TryParse(parts[4], out var v4)) e.helpRefused = v4;
                if (long.TryParse(parts[5], out var v5)) e.lastSeenMs = v5;
                _entries[parts[0]] = e;
            }
        }
    }
}
