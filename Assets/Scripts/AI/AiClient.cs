using System;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

namespace BlindLife.AI
{
    /// <summary>
    /// Two-mode AI client (mirrors the Canvas-era AiClient).
    ///
    /// 1. DIRECT GEMINI (recommended): paste a Google AI Studio key in
    ///    settings; the game calls generativelanguage.googleapis.com
    ///    directly. The key is stored only on device and only sent to
    ///    Google.
    ///
    /// 2. PROXY: a custom POST endpoint that returns {"reply":"..."}.
    ///
    /// If neither is configured, callers must use the offline fallback.
    /// </summary>
    public class AiClient
    {
        public enum Mode { Offline, Gemini, Proxy }

        public delegate void ReplyHandler(string text);
        public delegate void ErrorHandler(string msg);

        readonly Mode _mode;
        readonly string _geminiKey;
        readonly string _geminiModel;
        readonly string _proxyUrl;
        readonly MonoBehaviour _runner;

        public Mode CurrentMode => _mode;
        public bool IsConfigured => _mode != Mode.Offline;

        public static AiClient FromPrefs(MonoBehaviour runner, string geminiKey, string model, string proxyUrl)
        {
            if (!string.IsNullOrWhiteSpace(geminiKey))
                return new AiClient(runner, Mode.Gemini, geminiKey.Trim(),
                    string.IsNullOrWhiteSpace(model) ? "gemini-2.5-flash" : model.Trim(), "");
            if (!string.IsNullOrWhiteSpace(proxyUrl))
                return new AiClient(runner, Mode.Proxy, "", "", proxyUrl.Trim());
            return new AiClient(runner, Mode.Offline, "", "", "");
        }

        AiClient(MonoBehaviour runner, Mode mode, string key, string model, string proxy)
        {
            _runner = runner;
            _mode = mode;
            _geminiKey = key;
            _geminiModel = model;
            _proxyUrl = proxy;
        }

        public void Ask(string system, string user, ReplyHandler onReply, ErrorHandler onError = null)
        {
            if (_mode == Mode.Offline) { onError?.Invoke("offline"); return; }
            if (_runner == null) { onError?.Invoke("no_runner"); return; }
            _runner.StartCoroutine(_mode == Mode.Gemini
                ? CallGemini(system, user, onReply, onError)
                : CallProxy(system, user, onReply, onError));
        }

        System.Collections.IEnumerator CallGemini(string system, string user,
            ReplyHandler onReply, ErrorHandler onError)
        {
            string url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + UnityWebRequest.EscapeURL(_geminiModel) + ":generateContent?key="
                + UnityWebRequest.EscapeURL(_geminiKey);
            string body = BuildGeminiBody(system, user);
            using var req = new UnityWebRequest(url, "POST");
            req.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(body));
            req.downloadHandler = new DownloadHandlerBuffer();
            req.SetRequestHeader("Content-Type", "application/json; charset=utf-8");
            req.timeout = 20;
            yield return req.SendWebRequest();
            if (req.result != UnityWebRequest.Result.Success)
            {
                onError?.Invoke("http_" + req.responseCode);
                yield break;
            }
            string text = ExtractGeminiText(req.downloadHandler.text);
            if (string.IsNullOrWhiteSpace(text)) { onError?.Invoke("empty_reply"); yield break; }
            onReply?.Invoke(text);
        }

        System.Collections.IEnumerator CallProxy(string system, string user,
            ReplyHandler onReply, ErrorHandler onError)
        {
            string body = "{\"system\":" + Json(system) + ",\"user\":" + Json(user) + ",\"max_tokens\":220}";
            using var req = new UnityWebRequest(_proxyUrl, "POST");
            req.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(body));
            req.downloadHandler = new DownloadHandlerBuffer();
            req.SetRequestHeader("Content-Type", "application/json; charset=utf-8");
            req.timeout = 20;
            yield return req.SendWebRequest();
            if (req.result != UnityWebRequest.Result.Success)
            {
                onError?.Invoke("http_" + req.responseCode);
                yield break;
            }
            string text = ExtractProxyText(req.downloadHandler.text);
            if (string.IsNullOrWhiteSpace(text)) { onError?.Invoke("empty_reply"); yield break; }
            onReply?.Invoke(text);
        }

        // --- helpers ---

        static string BuildGeminiBody(string system, string user)
        {
            var sb = new StringBuilder();
            sb.Append('{');
            sb.Append("\"systemInstruction\":{\"parts\":[{\"text\":").Append(Json(system ?? "")).Append("}]},");
            sb.Append("\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":").Append(Json(user ?? "")).Append("}]}],");
            sb.Append("\"generationConfig\":{\"maxOutputTokens\":256,\"temperature\":0.9}");
            sb.Append('}');
            return sb.ToString();
        }

        static string ExtractGeminiText(string json)
        {
            // Pull "text":"..." from the first candidate part. Crude but safe.
            int i = json.IndexOf("\"candidates\"");
            if (i < 0) return null;
            i = json.IndexOf("\"parts\"", i);
            if (i < 0) return null;
            i = json.IndexOf("\"text\"", i);
            if (i < 0) return null;
            i = json.IndexOf(':', i);
            if (i < 0) return null;
            while (i < json.Length && (char.IsWhiteSpace(json[i]) || json[i] == ':')) i++;
            if (i >= json.Length || json[i] != '"') return null;
            return ReadJsonString(json, i);
        }

        static string ExtractProxyText(string json)
        {
            int i = json.IndexOf("\"reply\"");
            if (i < 0) i = json.IndexOf("\"text\"");
            if (i < 0) return json;
            i = json.IndexOf(':', i);
            if (i < 0) return null;
            while (i < json.Length && (char.IsWhiteSpace(json[i]) || json[i] == ':')) i++;
            if (i >= json.Length || json[i] != '"') return null;
            return ReadJsonString(json, i);
        }

        static string ReadJsonString(string s, int startQuote)
        {
            var sb = new StringBuilder();
            int i = startQuote + 1;
            while (i < s.Length)
            {
                char c = s[i];
                if (c == '\\' && i + 1 < s.Length)
                {
                    char n = s[i + 1];
                    if (n == 'n') sb.Append('\n');
                    else if (n == 't') sb.Append('\t');
                    else if (n == 'r') sb.Append('\r');
                    else if (n == '"') sb.Append('"');
                    else if (n == '\\') sb.Append('\\');
                    else sb.Append(n);
                    i += 2;
                }
                else if (c == '"') break;
                else { sb.Append(c); i++; }
            }
            return sb.ToString();
        }

        static string Json(string s)
        {
            if (s == null) return "\"\"";
            var sb = new StringBuilder("\"");
            foreach (var c in s)
            {
                switch (c)
                {
                    case '"': sb.Append("\\\""); break;
                    case '\\': sb.Append("\\\\"); break;
                    case '\n': sb.Append("\\n"); break;
                    case '\r': sb.Append("\\r"); break;
                    case '\t': sb.Append("\\t"); break;
                    default:
                        if (c < 32) sb.AppendFormat("\\u{0:x4}", (int)c);
                        else sb.Append(c);
                        break;
                }
            }
            sb.Append('"');
            return sb.ToString();
        }
    }
}
