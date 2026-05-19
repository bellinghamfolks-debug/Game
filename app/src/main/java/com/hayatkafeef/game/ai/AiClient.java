package com.hayatkafeef.game.ai;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Optional online client. POSTs a small JSON payload to a user-provided
 * proxy that holds the Gemini / OpenAI key. The proxy is expected to
 * return JSON like: {"reply":"…"}.
 *
 * If no URL is configured, the game falls back to offline templates.
 */
public class AiClient {

    public interface Callback {
        void onReply(String text);
        void onError(String msg);
    }

    private final String endpoint;

    public AiClient(String endpoint) {
        this.endpoint = endpoint == null ? "" : endpoint.trim();
    }

    public boolean isConfigured() { return !endpoint.isEmpty(); }

    public void ask(final String system, final String user, final Callback cb) {
        if (!isConfigured()) { cb.onError("no_endpoint"); return; }
        new AsyncTask<Void, Void, String[]>() {
            @Override protected String[] doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    String payload = "{\"system\":" + jstr(system)
                            + ",\"user\":" + jstr(user)
                            + ",\"max_tokens\":220}";
                    try (OutputStream os = conn.getOutputStream()) { os.write(payload.getBytes("UTF-8")); }

                    int code = conn.getResponseCode();
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    if (code >= 200 && code < 300) {
                        String reply = extractField(sb.toString(), "reply");
                        if (reply == null) reply = sb.toString();
                        return new String[]{"ok", reply};
                    }
                    return new String[]{"err", "http_" + code};
                } catch (Exception e) {
                    Log.w("AiClient", "ask err: " + e);
                    return new String[]{"err", e.getClass().getSimpleName()};
                } finally {
                    if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
            @Override protected void onPostExecute(String[] r) {
                if ("ok".equals(r[0])) cb.onReply(r[1]); else cb.onError(r[1]);
            }
        }.execute();
    }

    public void test(Callback cb) { ask("ping", "ping", cb); }

    /** Tiny JSON escaper. */
    private static String jstr(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Naive JSON field extractor good enough for {"reply":"..."} responses. */
    private static String extractField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int i = json.indexOf(key);
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i++;
        // skip whitespace
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder out = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') out.append('\n');
                else if (n == 't') out.append('\t');
                else if (n == 'r') out.append('\r');
                else out.append(n);
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
