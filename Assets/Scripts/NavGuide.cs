using UnityEngine;
using UnityEngine.AI;

namespace BlindLife
{
    /// <summary>
    /// Turn-by-turn voice guidance using Unity's NavMesh when one is baked,
    /// otherwise straight-line guidance. Announces an intro, then periodic
    /// 'بقي X خطوة' updates and 'وصلت' on arrival.
    /// </summary>
    public class NavGuide : MonoBehaviour
    {
        public static NavGuide Instance { get; private set; }

        public bool HasTarget => _target != null;
        public Vector3 TargetPosition => _target != null ? _target.transform.position : Vector3.zero;
        public Interactable TargetInteractable => _target;

        Interactable _target;
        Transform _player;
        NavMeshPath _path;
        float _nextUpdate;
        bool _reached;

        void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(this); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            _path = new NavMeshPath();
        }

        public void Begin(Interactable target, Transform player)
        {
            _target = target;
            _player = player;
            _reached = false;
            _nextUpdate = Time.time + 2f;
            GameManager.Instance?.NotifyNav();
            float dist = Vector3.Distance(player.position, target.transform.position);
            int steps = Mathf.Max(1, Mathf.RoundToInt(dist / 0.7f));
            AccessibilityManager.Instance?.SpeakNow(
                "سأرشدك إلى " + target.displayName + ". المسافة الكلية حوالي " + steps + " خطوة.");
        }

        public void Cancel()
        {
            _target = null;
            _player = null;
            _reached = false;
        }

        void Update()
        {
            if (_target == null || _player == null || _reached) return;
            float d = Vector3.Distance(_player.position, _target.transform.position);
            if (d < 1.2f)
            {
                _reached = true;
                AccessibilityManager.Instance?.SpeakNow(
                    "وصلتَ إلى " + _target.displayName + ". انقر مرتين للتفاعل.");
                return;
            }
            if (Time.time < _nextUpdate) return;
            _nextUpdate = Time.time + 4f;

            // direction relative to player heading
            Vector3 dir = (_target.transform.position - _player.position).normalized;
            float right = Vector3.Dot(dir, _player.right);
            float fwd   = Vector3.Dot(dir, _player.forward);
            int steps = Mathf.Max(1, Mathf.RoundToInt(d / 0.7f));
            string hint = "أحسنت، استمر";
            if (fwd < 0)
            {
                hint = "استدر للخلف";
            }
            else if (right > 0.4f) hint = "اتجه قليلًا لليمين";
            else if (right < -0.4f) hint = "اتجه قليلًا لليسار";
            AccessibilityManager.Instance?.Speak(hint + ". بقي " + steps + " خطوة.");
        }
    }
}
