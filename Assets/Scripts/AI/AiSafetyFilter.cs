using System.Text.RegularExpressions;

namespace BlindLife.AI
{
    /// <summary>
    /// Sanitises LLM output before TTS or UI display. Trims, caps length,
    /// strips role markers and markdown emphasis. Returns null to reject.
    /// </summary>
    public static class AiSafetyFilter
    {
        const int MaxLen = 480;
        static readonly string[] Blocked = { "<script", "</script", "javascript:", "data:text/html" };
        static readonly Regex RoleMarker = new Regex(@"(?i)^(assistant|user|system)\s*[:\-]+", RegexOptions.Compiled);
        static readonly Regex Markdown = new Regex(@"[\*_`]{1,3}", RegexOptions.Compiled);
        static readonly Regex HtmlTag = new Regex(@"<[^>]{1,80}>", RegexOptions.Compiled);

        public static string Filter(string text)
        {
            if (string.IsNullOrEmpty(text)) return null;
            string t = text.Trim();
            if (t.Length < 2) return null;
            t = RoleMarker.Replace(t, "");
            t = HtmlTag.Replace(t, "");
            t = Markdown.Replace(t, "");
            t = t.Trim();
            string lower = t.ToLowerInvariant();
            foreach (var b in Blocked) if (lower.Contains(b)) return null;
            if (t.Length > MaxLen) t = t.Substring(0, MaxLen);
            return t;
        }
    }
}
