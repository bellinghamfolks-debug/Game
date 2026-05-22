using System.Collections.Generic;
using BlindLife.AI;
using BlindLife.Hazards;
using BlindLife.Missions;
using BlindLife.UI;
using BlindLife.Vision;
using BlindLife.World;
using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Central orchestrator: drives the in-game clock, missions, hazards,
    /// the AI managers, the event log, and pushes state into the HUD.
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        public GameState State { get; private set; } = new GameState();
        public MissionManager Missions { get; private set; }
        public EventLog Log { get; } = new EventLog();
        public AiClient Ai { get; private set; }
        public AiHintManager Hints { get; private set; }
        public AiSummaryManager Summaries { get; private set; }
        public AiDialogueManager Dialogues { get; private set; }
        public CharacterMemory Memory { get; } = new CharacterMemory();
        public HazardSystem Hazards { get; private set; }
        public HudController Hud { get; set; }
        public VisionFilter Vision { get; set; }
        public Transform Player { get; set; }

        const string PrefKeyGeminiKey   = "blindlife_gemini_key";
        const string PrefKeyGeminiModel = "blindlife_gemini_model";
        const string PrefKeyProxyUrl    = "blindlife_proxy_url";
        const string PrefKeyAiEnabled   = "blindlife_ai_enabled";
        const string PrefKeyAiMode      = "blindlife_ai_mode";
        const string PrefKeyDifficulty  = "blindlife_difficulty";
        const string PrefKeyVision      = "blindlife_vision_mode";

        float _lastClockTick;
        float _missionCheckAcc;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        public void OnWorldReady()
        {
            Memory.Load();
            BuildAi();
            Hazards = gameObject.AddComponent<HazardSystem>();
            Hazards.SetDifficulty(PlayerPrefs.GetInt(PrefKeyDifficulty, 1));
            Hazards.OnWarning  += OnHazardWarning;
            Hazards.OnResolve  += OnHazardResolved;

            int day = State.day;
            Missions = DayScript.ForDay(day);
            Missions.OnMissionCompleted += OnMissionCompleted;
            Missions.OnAllCompleted += OnAllMissionsCompleted;

            if (Player != null) State.playerTransform = Player;
            Missions.Prime(State);

            // Apply persisted vision mode
            int vm = PlayerPrefs.GetInt(PrefKeyVision, (int)VisionMode.Sighted);
            if (Vision != null) Vision.SetMode((VisionMode)vm);

            AccessibilityManager.Instance?.Speak(OfflineContent.DayIntro(day));
            var first = Missions.Current;
            if (first != null)
                AccessibilityManager.Instance?.Speak(OfflineContent.MissionStart(first));
        }

        public void BuildAi()
        {
            string key = PlayerPrefs.GetString(PrefKeyGeminiKey, "");
            string model = PlayerPrefs.GetString(PrefKeyGeminiModel, "gemini-2.5-flash");
            string proxy = PlayerPrefs.GetString(PrefKeyProxyUrl, "");
            Ai = AiClient.FromPrefs(this, key, model, proxy);
            Hints = new AiHintManager(Ai);
            Summaries = new AiSummaryManager(Ai);
            Dialogues = new AiDialogueManager(Ai, Memory);
        }

        public bool AiEnabled =>
            PlayerPrefs.GetInt(PrefKeyAiEnabled, 1) == 1 &&
            PlayerPrefs.GetInt(PrefKeyAiMode, 1) >= 1 &&
            Ai != null && Ai.IsConfigured;

        public void OnInteract(Interactable it)
        {
            if (it == null) return;
            if (!string.IsNullOrEmpty(it.id)) State.SetFlag("i:" + it.id);
            if (it.kind == InteractableKind.Person)
                Memory.RecordInteraction(it.id);
            Log.Add(State.FormatTime(), "تفاعل مع " + (it.displayName ?? it.id));
        }

        void OnMissionCompleted(Mission prev, Mission next)
        {
            string done = OfflineContent.MissionDone(prev);
            AccessibilityManager.Instance?.SpeakNow(done);
            Log.Add(State.FormatTime(), done);
            if (next != null)
            {
                AccessibilityManager.Instance?.Speak(OfflineContent.MissionStart(next));
                Log.Add(State.FormatTime(), "مهمة جديدة: " + next.title);
            }
            State.missionIdx = Missions.Progress;
        }

        void OnAllMissionsCompleted()
        {
            string msg = OfflineContent.DayComplete(State.day);
            AccessibilityManager.Instance?.SpeakNow(msg);
            Log.Add(State.FormatTime(), msg);
        }

        void OnHazardWarning(string text, string shortLabel, float window)
        {
            AccessibilityManager.Instance?.Danger(text);
            Log.Add(State.FormatTime(), text);
            Hud?.ShowHazardWarning(shortLabel, text, window);
        }

        void OnHazardResolved(string text, bool impact)
        {
            if (impact) AccessibilityManager.Instance?.Warn(text);
            else AccessibilityManager.Instance?.SpeakNow(text);
            Log.Add(State.FormatTime(), text);
            Hud?.ShowHazardResult(text, impact);
        }

        public void AdvanceDay()
        {
            State.day = Mathf.Min(7, State.day + 1);
            State.minutes = 0;
            State.missionIdx = 0;
            State.flags.Clear();
            Missions = DayScript.ForDay(State.day);
            Missions.OnMissionCompleted += OnMissionCompleted;
            Missions.OnAllCompleted += OnAllMissionsCompleted;
            Missions.Prime(State);
            Hazards?.Reset();
            string intro = OfflineContent.DayIntro(State.day);
            AccessibilityManager.Instance?.SpeakNow(intro);
            Log.Add(State.FormatTime(), intro);
            var first = Missions.Current;
            if (first != null)
                AccessibilityManager.Instance?.Speak(OfflineContent.MissionStart(first));
        }

        public void RequestHint()
        {
            if (Hints == null) return;
            Hints.Ask(State, Missions?.Current, AiEnabled, (text, fromAi) =>
            {
                AccessibilityManager.Instance?.SpeakNow(text);
                Log.Add(State.FormatTime(), "تلميح: " + text);
            });
        }

        public void RequestSummary()
        {
            if (Summaries == null) return;
            var recent = new List<string>();
            foreach (var e in Log.Recent(20)) recent.Add(e.text);
            Summaries.DaySummary(State, recent, AiEnabled, (text, fromAi) =>
            {
                AccessibilityManager.Instance?.Speak(text);
            });
        }

        // ---- per-frame tick ----
        void Update()
        {
            // In-game clock: 1 game minute = 6 real seconds.
            if (Time.time - _lastClockTick > 6f)
            {
                State.minutes++;
                _lastClockTick = Time.time;
            }
            // Mission check at 5 Hz to keep checks cheap.
            _missionCheckAcc += Time.deltaTime;
            if (_missionCheckAcc > 0.2f)
            {
                _missionCheckAcc = 0;
                Missions?.Check(State);
            }
            // HUD refresh
            Hud?.UpdateHud(State, Missions);
            // Hazards
            if (Hazards != null && State.playerTransform != null)
            {
                bool moving = false;
                var pc = State.playerTransform.GetComponent<PlayerController>();
                if (pc != null)
                {
                    var rb = pc.GetComponent<Rigidbody>();
                    if (rb != null) moving = rb.velocity.sqrMagnitude > 0.05f;
                }
                Hazards.Tick(moving, State);
            }
        }

        void OnApplicationQuit() { Memory.Save(); }
    }
}
