package com.hayatkafeef.game.ai;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Two-mode AI client.
 *
 * 1. DIRECT GEMINI (recommended for users): paste your Google AI Studio
 *    API key in Settings. We call generativelanguage.googleapis.com directly.
 *    Key is stored locally only and only sent to Google.
 *
 * 2. PROXY (advanced): a custom POST endpoint that returns {"reply":"..."}.
 *    Useful when you want the key to live on a server rather than the device.
 *
 * If neither is configured, the game runs fully offline using EnvDescriber.
 */
public class AiClient {

    private static final String TAG = "Hayat.Ai";

    public interface Callback {
        void onReply(String text);
        void onError(String msg);
    }

    public enum Mode { OFFLINE, GEMINI, PROXY }

    private final Mode mode;
    private final String geminiKey;
    private final String geminiModel;
    private final String proxyUrl;

    /** Build for direct Gemini. */
    public static AiClient gemini(String key, String model) {
        return new AiClient(Mode.GEMINI, key, model, "");
    }
    /** Build for proxy. */
    public static AiClient proxy(String url) {
        return new AiClient(Mode.PROXY, "", "", url);
    }
    /** Build from current prefs values, preferring Gemini key. */
    public static AiClient fromPrefs(String geminiKey, String model, String proxyUrl) {
        if (geminiKey != null && !geminiKey.trim().isEmpty()) {
            return gemini(geminiKey.trim(), (model == null || model.trim().isEmpty()) ? "gemini-2.5-flash" : model.trim());
        }
        if (proxyUrl != null && !proxyUrl.trim().isEmpty()) {
            return proxy(proxyUrl.trim());
        }
        return new AiClient(Mode.OFFLINE, "", "", "");
    }

    private AiClient(Mode mode, String geminiKey, String geminiModel, String proxyUrl) {
        this.mode = mode;
        this.geminiKey = geminiKey == null ? "" : geminiKey;
        this.geminiModel = geminiModel == null ? "" : geminiModel;
        this.proxyUrl = proxyUrl == null ? "" : proxyUrl;
    }

    public boolean isConfigured() { return mode != Mode.OFFLINE; }
    public Mode mode() { return mode; }

    public void ask(final String system, final String user, final Callback cb) {
        if (mode == Mode.OFFLINE) { cb.onError("no_endpoint"); return; }
        new AsyncTask<Void, Void, String[]>() {
            @Override protected String[] doInBackground(Void... voids) {
                try {
                    if (mode == Mode.GEMINI) return callGemini(system, user);
                    return callProxy(system, user);
                } catch (Exception e) {
                    Log.w(TAG, "ai err: " + e);
                    return new String[]{"err", e.getClass().getSimpleName()};
                }
            }
            @Override protected void onPostExecute(String[] r) {
                if ("ok".equals(r[0])) cb.onReply(r[1]); else cb.onError(r[1]);
            }
        }.execute();
    }

    /** A cheap connectivity test: tries a tiny request. */
    public void test(Callback cb) {
        ask("اختبار اتصال.", "قل: اتصال ناجح.", cb);
    }

    // --------- Gemini direct ---------

    private String[] callGemini(String system, String user) {
        HttpURLConnection conn = null;
        try {
            String model = geminiModel.isEmpty() ? "gemini-2.5-flash" : geminiModel;
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + URLEncoder.encode(model, "UTF-8") + ":generateContent?key="
                    + URLEncoder.encode(geminiKey, "UTF-8");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject body = new JSONObject();
            // system instruction
            JSONObject sysInstr = new JSONObject();
            JSONArray sysParts = new JSONArray();
            sysParts.put(new JSONObject().put("text", system == null ? "" : system));
            sysInstr.put("parts", sysParts);
            body.put("systemInstruction", sysInstr);

            // user content
            JSONObject userPart = new JSONObject().put("text", user == null ? "" : user);
            JSONObject userContent = new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(userPart));
            body.put("contents", new JSONArray().put(userContent));

            JSONObject genCfg = new JSONObject();
            genCfg.put("maxOutputTokens", 256);
            genCfg.put("temperature", 0.9);
            body.put("generationConfig", genCfg);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String raw = readAll(conn, code);

            if (code < 200 || code >= 300) {
                String msg = "http_" + code;
                try {
                    JSONObject err = new JSONObject(raw);
                    JSONObject e = err.optJSONObject("error");
                    if (e != null && e.has("message")) msg = e.getString("message");
                } catch (Exception ignored) {}
                return new String[]{"err", msg};
            }

            JSONObject parsed = new JSONObject(raw);
            JSONArray cands = parsed.optJSONArray("candidates");
            if (cands == null || cands.length() == 0) return new String[]{"err", "no_candidates"};
            JSONObject content = cands.getJSONObject(0).optJSONObject("content");
            if (content == null) return new String[]{"err", "no_content"};
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) return new String[]{"err", "no_parts"};
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                String t = parts.getJSONObject(i).optString("text", "");
                if (!t.isEmpty()) {
                    if (out.length() > 0) out.append("\n");
                    out.append(t);
                }
            }
            String text = out.toString().trim();
            if (text.isEmpty()) return new String[]{"err", "empty_reply"};
            return new String[]{"ok", text};
        } catch (Exception e) {
            Log.w(TAG, "gemini err: " + e);
            return new String[]{"err", e.getClass().getSimpleName()};
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    // --------- Proxy mode ---------

    private String[] callProxy(String system, String user) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(proxyUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject body = new JSONObject();
            body.put("system", system == null ? "" : system);
            body.put("user", user == null ? "" : user);
            body.put("max_tokens", 220);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String raw = readAll(conn, code);
            if (code < 200 || code >= 300) return new String[]{"err", "http_" + code};
            try {
                JSONObject parsed = new JSONObject(raw);
                if (parsed.has("reply")) return new String[]{"ok", parsed.getString("reply")};
                if (parsed.has("text")) return new String[]{"ok", parsed.getString("text")};
            } catch (Exception ignored) {}
            return new String[]{"ok", raw};
        } catch (Exception e) {
            Log.w(TAG, "proxy err: " + e);
            return new String[]{"err", e.getClass().getSimpleName()};
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    private static String readAll(HttpURLConnection conn, int code) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } catch (Exception ignored) {}
        return sb.toString();
    }
}
