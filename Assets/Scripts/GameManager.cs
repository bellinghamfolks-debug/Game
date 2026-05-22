using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Game-level singleton: missions, day/night, save/load,
    /// and the interactions log.
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        public int day = 1;
        public int minutes;       // since 06:00
        public int mobility = 30;
        public int social = 30;
        public int tech = 20;
        public int confidence = 25;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        public void OnWorldReady()
        {
            AccessibilityManager.Instance?.Speak("أهلًا بك في حياة كفيف. هذه نسخة Unity 3D. اسحب للأعلى للمشي، أو اضغط مطوّلًا لتسمع ما حولك.");
        }

        public void OnInteract(Interactable it)
        {
            // hook missions, achievements, character memory here.
        }

        void Update()
        {
            // advance in-game clock 1 minute every 6 real seconds
            minutes = Mathf.RoundToInt(Time.time / 6f);
        }
    }
}
