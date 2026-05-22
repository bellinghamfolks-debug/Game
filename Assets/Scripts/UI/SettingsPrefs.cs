using UnityEngine;

namespace BlindLife.UI
{
    /// <summary>
    /// Typed wrapper around PlayerPrefs for the settings UI. Centralises
    /// the key names so the UI and the game engine never disagree.
    /// </summary>
    public class SettingsPrefs
    {
        const string K_GeminiKey   = "blindlife_gemini_key";
        const string K_GeminiModel = "blindlife_gemini_model";
        const string K_ProxyUrl    = "blindlife_proxy_url";
        const string K_AiEnabled   = "blindlife_ai_enabled";
        const string K_AiMode      = "blindlife_ai_mode";
        const string K_Vision      = "blindlife_vision_mode";
        const string K_Difficulty  = "blindlife_difficulty";

        public string GeminiKey
        {
            get => PlayerPrefs.GetString(K_GeminiKey, "");
            set => PlayerPrefs.SetString(K_GeminiKey, value ?? "");
        }
        public string GeminiModel
        {
            get => PlayerPrefs.GetString(K_GeminiModel, "gemini-2.5-flash");
            set => PlayerPrefs.SetString(K_GeminiModel,
                string.IsNullOrWhiteSpace(value) ? "gemini-2.5-flash" : value.Trim());
        }
        public string ProxyUrl
        {
            get => PlayerPrefs.GetString(K_ProxyUrl, "");
            set => PlayerPrefs.SetString(K_ProxyUrl, value?.Trim() ?? "");
        }
        public bool AiEnabled
        {
            get => PlayerPrefs.GetInt(K_AiEnabled, 1) == 1;
            set => PlayerPrefs.SetInt(K_AiEnabled, value ? 1 : 0);
        }
        public int AiMode
        {
            get => PlayerPrefs.GetInt(K_AiMode, 1);
            set => PlayerPrefs.SetInt(K_AiMode, Mathf.Clamp(value, 0, 2));
        }
        /// <summary>0=Total .. 5=Sighted, matches BlindLife.Vision.VisionMode.</summary>
        public int Vision
        {
            get => PlayerPrefs.GetInt(K_Vision, 5);
            set => PlayerPrefs.SetInt(K_Vision, Mathf.Clamp(value, 0, 5));
        }
        public int Difficulty
        {
            get => PlayerPrefs.GetInt(K_Difficulty, 1);
            set => PlayerPrefs.SetInt(K_Difficulty, Mathf.Clamp(value, 0, 2));
        }

        public void Save() => PlayerPrefs.Save();
    }
}
