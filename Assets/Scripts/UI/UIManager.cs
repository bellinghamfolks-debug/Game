using System.Collections.Generic;
using BlindLife.Vision;
using UnityEngine;
using UnityEngine.UI;

namespace BlindLife.UI
{
    /// <summary>
    /// Owns the modal overlays (pause, nav, settings, dialogue,
    /// day-summary). Activity has been replaced by a single overlay
    /// canvas in 3D space — every overlay screen is a panel on this
    /// canvas, shown one at a time.
    /// </summary>
    public class UIManager : MonoBehaviour
    {
        public static UIManager Instance { get; private set; }

        Canvas _modalCanvas;
        GameObject _backdrop;
        Font _font;
        public bool IsModalOpen => _backdrop != null && _backdrop.activeSelf;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            _font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            BuildCanvas();
        }

        void BuildCanvas()
        {
            var go = new GameObject("ModalCanvas");
            go.transform.SetParent(transform);
            _modalCanvas = go.AddComponent<Canvas>();
            _modalCanvas.renderMode = RenderMode.ScreenSpaceOverlay;
            _modalCanvas.sortingOrder = 50;
            var scaler = go.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);
            go.AddComponent<GraphicRaycaster>();

            _backdrop = new GameObject("Backdrop");
            _backdrop.transform.SetParent(_modalCanvas.transform, false);
            var img = _backdrop.AddComponent<Image>();
            img.color = new Color(0, 0, 0, 0.88f);
            var rt = img.rectTransform;
            rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
            rt.offsetMin = Vector2.zero; rt.offsetMax = Vector2.zero;
            _backdrop.SetActive(false);
        }

        // ---------- public entry points ----------

        public void OpenPauseMenu() => ShowMenu("القائمة", "متوقفة",
            new (string, System.Action)[]
            {
                ("متابعة", Close),
                ("ما المهمة الآن؟", () => { Close(); GameManager.Instance?.Player?.GetComponent<PlayerController>()?.CmdMissionStatus(); }),
                ("ملخص اليوم", () => { Close(); GameManager.Instance?.RequestSummary(); }),
                ("سجل الأحداث", OpenEventLog),
                ("الإعدادات", OpenSettings),
                ("الدليل", OpenManual),
                ("خروج", () => Application.Quit())
            });

        public void OpenNavMenu()
        {
            var gm = GameManager.Instance;
            if (gm?.Player == null) return;

            // Build a list of all interactables in the world, sorted by distance.
            var all = FindObjectsOfType<Interactable>();
            System.Array.Sort(all, (a, b) => {
                float da = (a.transform.position - gm.Player.position).sqrMagnitude;
                float db = (b.transform.position - gm.Player.position).sqrMagnitude;
                return da.CompareTo(db);
            });
            var entries = new List<(string, System.Action)>();
            entries.Add(("إلغاء الإرشاد", () => { Close(); NavGuide.Instance?.Cancel(); }));
            int count = 0;
            foreach (var it in all)
            {
                if (count >= 12) break;
                float d = Vector3.Distance(gm.Player.position, it.transform.position);
                int steps = Mathf.Max(1, Mathf.RoundToInt(d / 0.7f));
                string label = (it.displayName ?? it.id) + " — " + steps + " خطوة";
                var captured = it;
                entries.Add((label, () =>
                {
                    Close();
                    NavGuide.Instance?.Begin(captured, gm.Player);
                }));
                count++;
            }
            ShowMenu("خذني إلى…", "اختر وجهة", entries.ToArray());
        }

        public void OpenSettings()
        {
            var prefs = new SettingsPrefs();
            ShowSettings(prefs);
        }

        public void OpenManual() => ShowText("الدليل",
            "التحكم:\n" +
            "• سحب للأعلى = مشي للأمام\n" +
            "• سحب للأسفل = توقف\n" +
            "• سحب يمين/يسار = استدارة\n" +
            "• نقرتان = تفاعل مع أقرب شيء\n" +
            "• ضغط مطوّل = وصف ما حولك\n\n" +
            "زر 🧭 = اختر وجهة وسأرشدك خطوة بخطوة\n" +
            "زر 💡 = تلميح ذكي عن المهمة الحالية\n" +
            "زر 🔊 = وصف البيئة\n" +
            "زر ✋ = تفاعل\n" +
            "زر ⏸ = القائمة (إعدادات، ملخص، سجل، خروج)\n\n" +
            "أوضاع الرؤية:\n" +
            "في الإعدادات تختار: مبصر، كفيف كلي، ضعف شديد، رؤية ضبابية، رؤية مركزية، رؤية طرفية.\n\n" +
            "الذكاء الاصطناعي:\n" +
            "احصل على مفتاح Gemini من aistudio.google.com والصقه في الإعدادات. سيُحفظ على جهازك فقط.");

        public void OpenEventLog()
        {
            string body = GameManager.Instance?.Log?.Render(15);
            if (string.IsNullOrWhiteSpace(body)) body = "لم يحدث شيء بعد.";
            ShowText("سجل الأحداث", body);
        }

        public void Close()
        {
            if (_backdrop == null) return;
            ClearChildren(_backdrop.transform);
            _backdrop.SetActive(false);
        }

        // ---------- builders ----------

        void ShowMenu(string title, string subtitle, (string, System.Action)[] options)
        {
            if (_backdrop == null) return;
            ClearChildren(_backdrop.transform);
            _backdrop.SetActive(true);

            var titleGo = MakeText(_backdrop.transform, title, 60, new Vector2(0, 1), new Vector2(1, 1));
            var titleRt = ((RectTransform)titleGo.transform);
            titleRt.pivot = new Vector2(0.5f, 1);
            titleRt.anchoredPosition = new Vector2(0, -60);
            titleRt.sizeDelta = new Vector2(0, 120);

            var subGo = MakeText(_backdrop.transform, subtitle, 30, new Vector2(0, 1), new Vector2(1, 1));
            var subRt = ((RectTransform)subGo.transform);
            subRt.pivot = new Vector2(0.5f, 1);
            subRt.anchoredPosition = new Vector2(0, -190);
            subRt.sizeDelta = new Vector2(0, 70);

            // Vertical button list, centered.
            var list = new GameObject("List");
            list.transform.SetParent(_backdrop.transform, false);
            var listRt = list.AddComponent<RectTransform>();
            listRt.anchorMin = new Vector2(0.05f, 0); listRt.anchorMax = new Vector2(0.95f, 1);
            listRt.offsetMin = new Vector2(0, 80); listRt.offsetMax = new Vector2(0, -280);
            var lay = list.AddComponent<VerticalLayoutGroup>();
            lay.spacing = 16;
            lay.childControlWidth = true;
            lay.childControlHeight = false;
            lay.childForceExpandWidth = true;

            foreach (var (label, action) in options)
                AddBigButton(list.transform, label, action);

            AccessibilityManager.Instance?.SpeakNow(title + ". " + subtitle);
        }

        void ShowText(string title, string body)
        {
            if (_backdrop == null) return;
            ClearChildren(_backdrop.transform);
            _backdrop.SetActive(true);

            var t = MakeText(_backdrop.transform, title, 56, new Vector2(0, 1), new Vector2(1, 1));
            ((RectTransform)t.transform).pivot = new Vector2(0.5f, 1);
            ((RectTransform)t.transform).anchoredPosition = new Vector2(0, -50);
            ((RectTransform)t.transform).sizeDelta = new Vector2(0, 120);

            var scrollGo = new GameObject("Scroll");
            scrollGo.transform.SetParent(_backdrop.transform, false);
            var sRt = scrollGo.AddComponent<RectTransform>();
            sRt.anchorMin = new Vector2(0.05f, 0); sRt.anchorMax = new Vector2(0.95f, 1);
            sRt.offsetMin = new Vector2(0, 220); sRt.offsetMax = new Vector2(0, -180);
            var bg = scrollGo.AddComponent<Image>();
            bg.color = new Color(0.08f, 0.10f, 0.14f, 0.95f);
            var scroll = scrollGo.AddComponent<ScrollRect>();

            var viewGo = new GameObject("Viewport");
            viewGo.transform.SetParent(scrollGo.transform, false);
            var vRt = viewGo.AddComponent<RectTransform>();
            vRt.anchorMin = Vector2.zero; vRt.anchorMax = Vector2.one;
            vRt.offsetMin = new Vector2(20, 20); vRt.offsetMax = new Vector2(-20, -20);
            viewGo.AddComponent<RectMask2D>();
            scroll.viewport = vRt;

            var contentGo = new GameObject("Content");
            contentGo.transform.SetParent(viewGo.transform, false);
            var cRt = contentGo.AddComponent<RectTransform>();
            cRt.anchorMin = new Vector2(0, 1); cRt.anchorMax = new Vector2(1, 1);
            cRt.pivot = new Vector2(0.5f, 1);
            cRt.anchoredPosition = Vector2.zero;
            var fitter = contentGo.AddComponent<ContentSizeFitter>();
            fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;
            var vlay = contentGo.AddComponent<VerticalLayoutGroup>();
            vlay.childControlWidth = true;
            vlay.childForceExpandWidth = true;
            scroll.content = cRt;

            var txtGo = new GameObject("Body");
            txtGo.transform.SetParent(contentGo.transform, false);
            var txt = txtGo.AddComponent<Text>();
            txt.font = _font;
            txt.text = body;
            txt.color = new Color(0.95f, 0.95f, 0.95f);
            txt.fontSize = 36;
            txt.alignment = TextAnchor.UpperRight;
            txt.horizontalOverflow = HorizontalWrapMode.Wrap;
            txt.verticalOverflow = VerticalWrapMode.Overflow;

            AddBigButton(_backdrop.transform, "إغلاق", Close, 80, anchorBottom: true);
            AccessibilityManager.Instance?.SpeakNow(title + ". " + body);
        }

        void ShowSettings(SettingsPrefs prefs)
        {
            if (_backdrop == null) return;
            ClearChildren(_backdrop.transform);
            _backdrop.SetActive(true);

            var t = MakeText(_backdrop.transform, "الإعدادات", 56, new Vector2(0, 1), new Vector2(1, 1));
            ((RectTransform)t.transform).pivot = new Vector2(0.5f, 1);
            ((RectTransform)t.transform).anchoredPosition = new Vector2(0, -50);
            ((RectTransform)t.transform).sizeDelta = new Vector2(0, 120);

            var scrollGo = new GameObject("Scroll");
            scrollGo.transform.SetParent(_backdrop.transform, false);
            var sRt = scrollGo.AddComponent<RectTransform>();
            sRt.anchorMin = new Vector2(0.05f, 0); sRt.anchorMax = new Vector2(0.95f, 1);
            sRt.offsetMin = new Vector2(0, 220); sRt.offsetMax = new Vector2(0, -180);
            var bg = scrollGo.AddComponent<Image>();
            bg.color = new Color(0.08f, 0.10f, 0.14f, 0.95f);
            var scroll = scrollGo.AddComponent<ScrollRect>();
            scroll.horizontal = false;

            var viewGo = new GameObject("Viewport");
            viewGo.transform.SetParent(scrollGo.transform, false);
            var vRt = viewGo.AddComponent<RectTransform>();
            vRt.anchorMin = Vector2.zero; vRt.anchorMax = Vector2.one;
            vRt.offsetMin = new Vector2(20, 20); vRt.offsetMax = new Vector2(-20, -20);
            viewGo.AddComponent<RectMask2D>();
            scroll.viewport = vRt;

            var content = new GameObject("Content");
            content.transform.SetParent(viewGo.transform, false);
            var cRt = content.AddComponent<RectTransform>();
            cRt.anchorMin = new Vector2(0, 1); cRt.anchorMax = new Vector2(1, 1);
            cRt.pivot = new Vector2(0.5f, 1);
            var fitter = content.AddComponent<ContentSizeFitter>();
            fitter.verticalFit = ContentSizeFitter.FitMode.PreferredSize;
            var vlay = content.AddComponent<VerticalLayoutGroup>();
            vlay.spacing = 16;
            vlay.padding = new RectOffset(10, 10, 10, 10);
            vlay.childControlWidth = true;
            vlay.childForceExpandWidth = true;
            scroll.content = cRt;

            // Sections — each returns a getter for the new value.
            var keyField   = AddLabeledInput(content.transform, "مفتاح Gemini (من aistudio.google.com)", prefs.GeminiKey, isPassword: true);
            var modelField = AddLabeledInput(content.transform, "موديل Gemini (افتراضي gemini-2.5-flash)", prefs.GeminiModel);
            var proxyField = AddLabeledInput(content.transform, "بروكسي مخصص (اختياري)", prefs.ProxyUrl);
            var aiEnabledTog = AddToggle(content.transform, "تفعيل الذكاء الاصطناعي", prefs.AiEnabled);
            var aiModePicker = AddPicker(content.transform, "مستوى الذكاء",
                new[] { "إيقاف", "بسيط", "متقدم" }, prefs.AiMode);
            var visionPicker = AddPicker(content.transform, "حالة الرؤية",
                new[] { "كفيف كلي", "ضعف شديد", "ضبابي", "مركزي", "طرفي", "مبصر" }, prefs.Vision);
            var diffPicker = AddPicker(content.transform, "الصعوبة",
                new[] { "سهل", "عادي", "صعب" }, prefs.Difficulty);

            // Save + close buttons at the bottom of the screen.
            AddBigButton(_backdrop.transform, "حفظ", () =>
            {
                prefs.GeminiKey   = keyField();
                prefs.GeminiModel = modelField();
                prefs.ProxyUrl    = proxyField();
                prefs.AiEnabled   = aiEnabledTog();
                prefs.AiMode      = aiModePicker();
                prefs.Vision      = visionPicker();
                prefs.Difficulty  = diffPicker();
                prefs.Save();
                ApplyAfterSave();
                Close();
                AccessibilityManager.Instance?.SpeakNow("تم حفظ الإعدادات.");
            }, 80, anchorBottom: true, primary: true);

            AccessibilityManager.Instance?.SpeakNow("الإعدادات.");
        }

        void ApplyAfterSave()
        {
            var gm = GameManager.Instance;
            if (gm == null) return;
            gm.BuildAi(); // re-create AI client with new key/model/proxy
            int diff = PlayerPrefs.GetInt("blindlife_difficulty", 1);
            gm.Hazards?.SetDifficulty(diff);
            int vm = PlayerPrefs.GetInt("blindlife_vision_mode", (int)VisionMode.Sighted);
            gm.Vision?.SetMode((VisionMode)vm);
        }

        // ---------- micro-widgets ----------

        System.Func<string> AddLabeledInput(Transform parent, string label, string value, bool isPassword = false)
        {
            var row = new GameObject("Row_" + label);
            row.transform.SetParent(parent, false);
            var lay = row.AddComponent<VerticalLayoutGroup>();
            lay.childForceExpandWidth = true;
            lay.spacing = 4;
            row.AddComponent<LayoutElement>().minHeight = 130;

            var lblGo = new GameObject("Label");
            lblGo.transform.SetParent(row.transform, false);
            var lbl = lblGo.AddComponent<Text>();
            lbl.font = _font; lbl.fontSize = 28; lbl.color = new Color(0.9f, 0.82f, 0.45f);
            lbl.text = label;
            lbl.alignment = TextAnchor.MiddleRight;
            lblGo.AddComponent<LayoutElement>().minHeight = 40;

            var inputGo = new GameObject("Input");
            inputGo.transform.SetParent(row.transform, false);
            var img = inputGo.AddComponent<Image>();
            img.color = new Color(0.15f, 0.17f, 0.22f);
            var input = inputGo.AddComponent<InputField>();
            inputGo.AddComponent<LayoutElement>().minHeight = 80;
            input.contentType = isPassword
                ? InputField.ContentType.Password
                : InputField.ContentType.Standard;
            input.text = value ?? "";

            var textGo = new GameObject("Text");
            textGo.transform.SetParent(inputGo.transform, false);
            var t = textGo.AddComponent<Text>();
            t.font = _font; t.fontSize = 30; t.color = Color.white;
            t.supportRichText = false; t.alignment = TextAnchor.MiddleRight;
            var tRt = t.rectTransform;
            tRt.anchorMin = Vector2.zero; tRt.anchorMax = Vector2.one;
            tRt.offsetMin = new Vector2(20, 0); tRt.offsetMax = new Vector2(-20, 0);
            input.textComponent = t;

            return () => input.text;
        }

        System.Func<bool> AddToggle(Transform parent, string label, bool value)
        {
            var go = new GameObject("Toggle_" + label);
            go.transform.SetParent(parent, false);
            go.AddComponent<LayoutElement>().minHeight = 80;
            var img = go.AddComponent<Image>();
            img.color = new Color(0.15f, 0.17f, 0.22f);
            var tg = go.AddComponent<Toggle>();
            tg.isOn = value;

            var t = new GameObject("Label");
            t.transform.SetParent(go.transform, false);
            var tx = t.AddComponent<Text>();
            tx.font = _font; tx.fontSize = 30;
            tx.text = (value ? "✓ " : "○ ") + label;
            tx.color = Color.white;
            tx.alignment = TextAnchor.MiddleRight;
            var tRt = tx.rectTransform;
            tRt.anchorMin = Vector2.zero; tRt.anchorMax = Vector2.one;
            tRt.offsetMin = new Vector2(30, 0); tRt.offsetMax = new Vector2(-30, 0);
            tg.onValueChanged.AddListener(v => tx.text = (v ? "✓ " : "○ ") + label);

            return () => tg.isOn;
        }

        System.Func<int> AddPicker(Transform parent, string label, string[] options, int selected)
        {
            var row = new GameObject("Picker_" + label);
            row.transform.SetParent(parent, false);
            var lay = row.AddComponent<VerticalLayoutGroup>();
            lay.spacing = 4;
            row.AddComponent<LayoutElement>().minHeight = 80 + 70 * options.Length;

            var lblGo = new GameObject("Label");
            lblGo.transform.SetParent(row.transform, false);
            var lbl = lblGo.AddComponent<Text>();
            lbl.font = _font; lbl.fontSize = 28; lbl.color = new Color(0.9f, 0.82f, 0.45f);
            lbl.text = label; lbl.alignment = TextAnchor.MiddleRight;
            lblGo.AddComponent<LayoutElement>().minHeight = 40;

            int currentSel = Mathf.Clamp(selected, 0, options.Length - 1);
            Text[] btnLabels = new Text[options.Length];

            for (int i = 0; i < options.Length; i++)
            {
                int idx = i;
                var btn = new GameObject("Opt_" + i);
                btn.transform.SetParent(row.transform, false);
                btn.AddComponent<LayoutElement>().minHeight = 64;
                var bImg = btn.AddComponent<Image>();
                bImg.color = (idx == currentSel) ? new Color(0.95f, 0.8f, 0.4f) : new Color(0.15f, 0.17f, 0.22f);
                var b = btn.AddComponent<Button>();
                var bTxtGo = new GameObject("T");
                bTxtGo.transform.SetParent(btn.transform, false);
                var bTxt = bTxtGo.AddComponent<Text>();
                bTxt.font = _font; bTxt.fontSize = 30;
                bTxt.text = options[i];
                bTxt.color = (idx == currentSel) ? Color.black : Color.white;
                bTxt.alignment = TextAnchor.MiddleCenter;
                var bRt = bTxt.rectTransform;
                bRt.anchorMin = Vector2.zero; bRt.anchorMax = Vector2.one;
                bRt.offsetMin = Vector2.zero; bRt.offsetMax = Vector2.zero;
                btnLabels[i] = bTxt;
                b.onClick.AddListener(() =>
                {
                    currentSel = idx;
                    for (int j = 0; j < options.Length; j++)
                    {
                        var im = btnLabels[j].transform.parent.GetComponent<Image>();
                        im.color = (j == currentSel) ? new Color(0.95f, 0.8f, 0.4f) : new Color(0.15f, 0.17f, 0.22f);
                        btnLabels[j].color = (j == currentSel) ? Color.black : Color.white;
                    }
                });
            }
            return () => currentSel;
        }

        void AddBigButton(Transform parent, string label, System.Action onClick,
            float bottomOffset = 0, bool anchorBottom = false, bool primary = false)
        {
            var go = new GameObject("Btn_" + label);
            go.transform.SetParent(parent, false);
            var img = go.AddComponent<Image>();
            img.color = primary ? new Color(0.95f, 0.8f, 0.4f) : new Color(0.15f, 0.17f, 0.22f);
            var rt = img.rectTransform;
            if (anchorBottom)
            {
                rt.anchorMin = new Vector2(0.1f, 0); rt.anchorMax = new Vector2(0.9f, 0);
                rt.pivot = new Vector2(0.5f, 0);
                rt.sizeDelta = new Vector2(0, 100);
                rt.anchoredPosition = new Vector2(0, 30 + bottomOffset);
            }
            else
            {
                go.AddComponent<LayoutElement>().minHeight = 96;
            }
            var btn = go.AddComponent<Button>();
            btn.onClick.AddListener(() => onClick?.Invoke());

            var txtGo = new GameObject("T");
            txtGo.transform.SetParent(go.transform, false);
            var txt = txtGo.AddComponent<Text>();
            txt.font = _font; txt.fontSize = 36;
            txt.text = label;
            txt.color = primary ? Color.black : Color.white;
            txt.alignment = TextAnchor.MiddleCenter;
            var tRt = txt.rectTransform;
            tRt.anchorMin = Vector2.zero; tRt.anchorMax = Vector2.one;
            tRt.offsetMin = Vector2.zero; tRt.offsetMax = Vector2.zero;
        }

        GameObject MakeText(Transform parent, string text, int size, Vector2 anchorMin, Vector2 anchorMax)
        {
            var go = new GameObject("Text");
            go.transform.SetParent(parent, false);
            var t = go.AddComponent<Text>();
            t.font = _font; t.fontSize = size;
            t.text = text;
            t.color = new Color(0.95f, 0.85f, 0.45f);
            t.alignment = TextAnchor.MiddleCenter;
            t.fontStyle = FontStyle.Bold;
            var rt = t.rectTransform;
            rt.anchorMin = anchorMin; rt.anchorMax = anchorMax;
            return go;
        }

        static void ClearChildren(Transform t)
        {
            for (int i = t.childCount - 1; i >= 0; i--) Destroy(t.GetChild(i).gameObject);
        }
    }
}
