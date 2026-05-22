using System.Collections.Generic;
using System.Text;

namespace BlindLife.World
{
    /// <summary>
    /// Rolling 60-entry log of meaningful in-game events used by
    /// the Event Log overlay and as input to AI summaries.
    /// </summary>
    public class EventLog
    {
        public struct Entry
        {
            public string time;
            public string text;
        }

        const int Max = 60;
        readonly LinkedList<Entry> _entries = new LinkedList<Entry>();
        public Entry? Last { get; private set; }

        public void Add(string time, string text)
        {
            if (string.IsNullOrWhiteSpace(text)) return;
            if (_entries.Count >= Max) _entries.RemoveFirst();
            var e = new Entry { time = time, text = text.Trim() };
            _entries.AddLast(e);
            Last = e;
        }

        public List<Entry> Recent(int n)
        {
            var list = new List<Entry>(_entries);
            if (list.Count > n) list = list.GetRange(list.Count - n, n);
            return list;
        }

        public string Render(int n)
        {
            var sb = new StringBuilder();
            foreach (var e in Recent(n))
            {
                sb.Append(e.time).Append(" — ").Append(e.text).Append('\n');
            }
            return sb.ToString().Trim();
        }
    }
}
