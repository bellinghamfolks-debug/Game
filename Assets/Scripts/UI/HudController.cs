using BlindLife.Missions;
using UnityEngine;
using UnityEngine.UI;

namespace BlindLife.UI
{
    /// <summary>
    /// Minimal HUD: top strip with location + time + stats + current
    /// mission, plus a hazard warning banner that pulses when a hazard
    /// is armed.
    /// </summary>
    public class HudController : MonoBehaviour
    {
        Text _topText;
        Text _missionText;
        GameObject _hazardBanner;
        Text _hazardTitle;
        Text _hazardSub;

        void Awake() { BuildUi(); }

        void BuildUi()
        {
            var canvasGo = new GameObject("HudCanvas");
            canvasGo.transform.SetParent(transform);
            var canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 10;
            var scaler = canvasGo.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);

            // Top bar
            var top = new GameObject("Top");
            top.transform.SetParent(canvasGo.transform, false);
            var topImg = top.AddComponent<Image>();
            topImg.color = new Color(0, 0, 0, 0.6f);
            var topRt = topImg.rectTransform;
            topRt.anchorMin = new Vector2(0, 1); topRt.anchorMax = new Vector2(1, 1);
            topRt.pivot = new Vector2(0.5f, 1);
            topRt.sizeDelta = new Vector2(0, 180);
            topRt.anchoredPosition = Vector2.zero;

            var topTextGo = new GameObject("TopText");
            topTextGo.transform.SetParent(top.transform, false);
            _topText = topTextGo.AddComponent<Text>();
            _topText.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            _topText.color = new Color(1, 0.9f, 0.6f);
            _topText.fontSize = 28;
            _topText.alignment = TextAnchor.UpperRight;
            var ttRt = _topText.rectTransform;
            ttRt.anchorMin = Vector2.zero; ttRt.anchorMax = Vector2.one;
            ttRt.offsetMin = new Vector2(20, 8); ttRt.offsetMax = new Vector2(-20, -8);

            var missionGo = new GameObject("Mission");
            missionGo.transform.SetParent(top.transform, false);
            _missionText = missionGo.AddComponent<Text>();
            _missionText.font = _topText.font;
            _missionText.color = new Color(1, 0.85f, 0.4f);
            _missionText.fontSize = 32;
            _missionText.alignment = TextAnchor.LowerCenter;
            _missionText.fontStyle = FontStyle.Bold;
            var mRt = _missionText.rectTransform;
            mRt.anchorMin = Vector2.zero; mRt.anchorMax = Vector2.one;
            mRt.offsetMin = new Vector2(20, 8); mRt.offsetMax = new Vector2(-20, -8);

            // Hazard banner
            _hazardBanner = new GameObject("HazardBanner");
            _hazardBanner.transform.SetParent(canvasGo.transform, false);
            var hImg = _hazardBanner.AddComponent<Image>();
            hImg.color = new Color(0.55f, 0.12f, 0.12f, 0.85f);
            var hRt = hImg.rectTransform;
            hRt.anchorMin = new Vector2(0, 0.4f); hRt.anchorMax = new Vector2(1, 0.6f);
            hRt.offsetMin = Vector2.zero; hRt.offsetMax = Vector2.zero;

            var hTitleGo = new GameObject("HazardTitle");
            hTitleGo.transform.SetParent(_hazardBanner.transform, false);
            _hazardTitle = hTitleGo.AddComponent<Text>();
            _hazardTitle.font = _topText.font;
            _hazardTitle.color = Color.white;
            _hazardTitle.fontSize = 80;
            _hazardTitle.fontStyle = FontStyle.Bold;
            _hazardTitle.alignment = TextAnchor.MiddleCenter;
            var htRt = _hazardTitle.rectTransform;
            htRt.anchorMin = Vector2.zero; htRt.anchorMax = Vector2.one;
            htRt.offsetMin = Vector2.zero; htRt.offsetMax = new Vector2(0, 80);

            var hSubGo = new GameObject("HazardSub");
            hSubGo.transform.SetParent(_hazardBanner.transform, false);
            _hazardSub = hSubGo.AddComponent<Text>();
            _hazardSub.font = _topText.font;
            _hazardSub.color = Color.white;
            _hazardSub.fontSize = 36;
            _hazardSub.alignment = TextAnchor.MiddleCenter;
            var hsRt = _hazardSub.rectTransform;
            hsRt.anchorMin = Vector2.zero; hsRt.anchorMax = Vector2.one;
            hsRt.offsetMin = new Vector2(20, -100); hsRt.offsetMax = new Vector2(-20, 0);

            _hazardBanner.SetActive(false);
        }

        public void UpdateHud(GameState gs, MissionManager missions)
        {
            if (gs == null) return;
            string sceneName = "العالم";
            _topText.text =
                "اليوم " + gs.day + "  ·  " + gs.FormatTime() + "\n" +
                "حركة " + gs.mobility + "  ·  علاقات " + gs.social + "  ·  تقنية " + gs.tech + "  ·  ثقة " + gs.confidence;
            if (missions != null)
            {
                var cur = missions.Current;
                if (cur != null)
                    _missionText.text = "◉ " + (missions.Progress + 1) + "/" + missions.Total + " — " + cur.title;
                else
                    _missionText.text = "✓ اكتملت مهام اليوم";
            }
        }

        public void ShowHazardWarning(string title, string subtitle, float duration)
        {
            if (_hazardBanner == null) return;
            _hazardBanner.SetActive(true);
            _hazardTitle.text = "توقف! " + title;
            _hazardSub.text = subtitle;
            CancelInvoke(nameof(HideHazard));
            Invoke(nameof(HideHazard), duration + 1f);
        }

        public void ShowHazardResult(string text, bool impact)
        {
            if (_hazardBanner == null) return;
            _hazardTitle.text = impact ? "ارتطام!" : "نجوت!";
            _hazardSub.text = text;
            CancelInvoke(nameof(HideHazard));
            Invoke(nameof(HideHazard), 1.2f);
        }

        void HideHazard()
        {
            if (_hazardBanner != null) _hazardBanner.SetActive(false);
        }
    }
}
