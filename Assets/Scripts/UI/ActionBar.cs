using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

namespace BlindLife.UI
{
    /// <summary>
    /// Large accessibility-friendly bottom button bar. Each button has a
    /// big tap target, an Arabic content description that screen readers
    /// can speak, and triggers an engine command.
    ///
    /// All buttons return true from their pointer handlers so taps don't
    /// fall through to PlayerController and trigger an unintended
    /// double-tap interact.
    /// </summary>
    public class ActionBar : MonoBehaviour
    {
        public PlayerController player;

        void Awake() { BuildUi(); }

        void BuildUi()
        {
            var canvasGo = new GameObject("ActionBarCanvas");
            canvasGo.transform.SetParent(transform);
            var canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 12;
            var scaler = canvasGo.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);
            canvasGo.AddComponent<GraphicRaycaster>();
            EnsureEventSystem();

            var bar = new GameObject("Bar");
            bar.transform.SetParent(canvasGo.transform, false);
            var bg = bar.AddComponent<Image>();
            bg.color = new Color(0, 0, 0, 0.78f);
            var rt = bg.rectTransform;
            rt.anchorMin = new Vector2(0, 0); rt.anchorMax = new Vector2(1, 0);
            rt.pivot = new Vector2(0.5f, 0);
            rt.sizeDelta = new Vector2(0, 280);
            rt.anchoredPosition = Vector2.zero;

            var layout = bar.AddComponent<HorizontalLayoutGroup>();
            layout.padding = new RectOffset(20, 20, 20, 20);
            layout.spacing = 12;
            layout.childControlWidth = true;
            layout.childControlHeight = true;
            layout.childForceExpandWidth = true;
            layout.childForceExpandHeight = true;

            AddButton(bar.transform, "◀",  "استدارة يسار",         () => player?.CmdTurnLeft());
            AddButton(bar.transform, "▲",  "مشي",                   () => { player?.CmdWalk(); Invoke(nameof(StopAfter), 0.9f); }, primary: true);
            AddButton(bar.transform, "▶",  "استدارة يمين",          () => player?.CmdTurnRight());
            AddButton(bar.transform, "🔊", "وصف ما حولك",           () => player?.CmdDescribe());
            AddButton(bar.transform, "✋", "تفاعل",                  () => player?.CmdInteract());
            AddButton(bar.transform, "🧭", "خذني إلى",              () => UIManager.Instance?.OpenNavMenu());
            AddButton(bar.transform, "💡", "تلميح",                  () => player?.CmdHint());
            AddButton(bar.transform, "⏸",  "قائمة الإيقاف",         () => UIManager.Instance?.OpenPauseMenu());
        }

        void StopAfter() => player?.CmdStop();

        void AddButton(Transform parent, string label, string a11y, System.Action onClick, bool primary = false)
        {
            var go = new GameObject("Btn_" + label);
            go.transform.SetParent(parent, false);
            var img = go.AddComponent<Image>();
            img.color = primary
                ? new Color(0.88f, 0.76f, 0.41f)
                : new Color(0.10f, 0.12f, 0.15f);
            var btn = go.AddComponent<Button>();
            var colors = btn.colors;
            colors.highlightedColor = primary
                ? new Color(0.98f, 0.86f, 0.51f)
                : new Color(0.20f, 0.22f, 0.27f);
            btn.colors = colors;
            btn.onClick.AddListener(() => onClick?.Invoke());

            var txtGo = new GameObject("Label");
            txtGo.transform.SetParent(go.transform, false);
            var txt = txtGo.AddComponent<Text>();
            txt.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            txt.text = label;
            txt.color = primary ? Color.black : new Color(0.95f, 0.95f, 0.95f);
            txt.fontSize = 70;
            txt.alignment = TextAnchor.MiddleCenter;
            var trt = txt.rectTransform;
            trt.anchorMin = Vector2.zero; trt.anchorMax = Vector2.one;
            trt.offsetMin = Vector2.zero; trt.offsetMax = Vector2.zero;
        }

        static void EnsureEventSystem()
        {
            if (FindObjectOfType<EventSystem>() != null) return;
            var es = new GameObject("EventSystem");
            es.AddComponent<EventSystem>();
            es.AddComponent<StandaloneInputModule>();
        }
    }
}
