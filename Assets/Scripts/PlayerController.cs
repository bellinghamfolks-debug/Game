using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Player movement, gesture handling and the bridge between the
    /// world and the accessibility/audio layer.
    ///
    /// Controls:
    ///   - Swipe up:    walk forward
    ///   - Swipe down:  stop
    ///   - Swipe left:  turn left
    ///   - Swipe right: turn right
    ///   - Double tap:  interact with the closest thing
    ///   - Long press:  speak the environment description
    /// </summary>
    [RequireComponent(typeof(Rigidbody))]
    public class PlayerController : MonoBehaviour
    {
        public float walkSpeed = 2.0f;
        public float turnStepDegrees = 22.5f;

        Rigidbody _rb;
        InteractionFinder _interact;
        bool _walking;
        float _heading; // radians

        // gesture detection
        Vector2 _touchStart;
        float _touchStartTime;
        const float SwipeThresholdPx = 80f;
        const float LongPressSeconds = 0.55f;
        float _lastTapTime;
        const float DoubleTapWindow = 0.35f;

        void Awake()
        {
            _rb = GetComponent<Rigidbody>();
            _interact = GetComponent<InteractionFinder>();
        }

        void Update()
        {
            HandleTouch();
            HandleKeyboard();
        }

        void FixedUpdate()
        {
            if (!_walking) return;
            Vector3 forward = new Vector3(Mathf.Cos(_heading), 0f, Mathf.Sin(_heading));
            _rb.MovePosition(_rb.position + forward * walkSpeed * Time.fixedDeltaTime);
            // make the body face the heading visually
            Quaternion q = Quaternion.LookRotation(forward, Vector3.up);
            _rb.MoveRotation(Quaternion.Slerp(_rb.rotation, q, 6f * Time.fixedDeltaTime));
        }

        void HandleTouch()
        {
            if (Input.touchCount == 0) return;
            // Block world touches while any modal overlay is up.
            if (BlindLife.UI.UIManager.Instance != null && BlindLife.UI.UIManager.Instance.IsModalOpen) return;
            // Ignore touches that hit a UI element (button bar, etc).
            if (UnityEngine.EventSystems.EventSystem.current != null &&
                UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject(Input.GetTouch(0).fingerId)) return;
            var t = Input.GetTouch(0);
            switch (t.phase)
            {
                case TouchPhase.Began:
                    _touchStart = t.position;
                    _touchStartTime = Time.unscaledTime;
                    break;
                case TouchPhase.Stationary:
                    if (Time.unscaledTime - _touchStartTime > LongPressSeconds &&
                        (t.position - _touchStart).sqrMagnitude < SwipeThresholdPx * SwipeThresholdPx)
                    {
                        CmdDescribe();
                        _touchStartTime = float.MaxValue; // mark consumed
                    }
                    break;
                case TouchPhase.Ended:
                    Vector2 delta = t.position - _touchStart;
                    float dt = Time.unscaledTime - _touchStartTime;
                    if (delta.sqrMagnitude > SwipeThresholdPx * SwipeThresholdPx)
                    {
                        if (Mathf.Abs(delta.x) > Mathf.Abs(delta.y))
                        {
                            if (delta.x > 0) CmdTurnRight(); else CmdTurnLeft();
                        }
                        else
                        {
                            if (delta.y > 0) CmdWalk(); else CmdStop();
                        }
                    }
                    else if (dt < LongPressSeconds)
                    {
                        // tap — possibly double
                        if (Time.unscaledTime - _lastTapTime < DoubleTapWindow)
                        {
                            CmdInteract();
                            _lastTapTime = 0;
                        }
                        else _lastTapTime = Time.unscaledTime;
                    }
                    break;
            }
        }

        void HandleKeyboard()
        {
            if (Input.GetKeyDown(KeyCode.UpArrow))    CmdWalk();
            if (Input.GetKeyDown(KeyCode.DownArrow))  CmdStop();
            if (Input.GetKeyDown(KeyCode.LeftArrow))  CmdTurnLeft();
            if (Input.GetKeyDown(KeyCode.RightArrow)) CmdTurnRight();
            if (Input.GetKeyDown(KeyCode.Space))      CmdInteract();
            if (Input.GetKeyDown(KeyCode.Tab))        CmdDescribe();
            if (Input.GetKeyDown(KeyCode.H))          CmdHint();
            if (Input.GetKeyDown(KeyCode.M))          CmdMissionStatus();
            if (Input.GetKeyDown(KeyCode.Y))          CmdDaySummary();
            if (Input.GetKeyDown(KeyCode.R))          CmdRepeat();
        }

        public void CmdHint()    => GameManager.Instance?.RequestHint();
        public void CmdMissionStatus()
        {
            var m = GameManager.Instance?.Missions?.Current;
            if (m == null) AccessibilityManager.Instance?.SpeakNow("اكتملت جميع مهام اليوم.");
            else AccessibilityManager.Instance?.SpeakNow(
                "المهمة الحالية: " + m.title + ". " + m.description);
        }
        public void CmdDaySummary() => GameManager.Instance?.RequestSummary();
        public void CmdRepeat()
        {
            var last = GameManager.Instance?.Log?.Last;
            AccessibilityManager.Instance?.SpeakNow(
                last.HasValue ? last.Value.text : "لا يوجد ما أكرره الآن.");
        }

        public void CmdWalk()
        {
            _walking = true;
            AccessibilityManager.Instance?.SpeakNow("مشي");
            AccessibilityManager.Instance?.Confirm();
        }
        public void CmdStop()
        {
            _walking = false;
            AccessibilityManager.Instance?.SpeakNow("توقف");
            AccessibilityManager.Instance?.Confirm();
        }
        public void CmdTurnLeft()
        {
            _heading -= turnStepDegrees * Mathf.Deg2Rad;
            AccessibilityManager.Instance?.SpeakNow("يسار");
            AccessibilityManager.Instance?.Confirm();
        }
        public void CmdTurnRight()
        {
            _heading += turnStepDegrees * Mathf.Deg2Rad;
            AccessibilityManager.Instance?.SpeakNow("يمين");
            AccessibilityManager.Instance?.Confirm();
        }
        public void CmdInteract()
        {
            if (_interact != null) _interact.TryInteract();
        }
        public void CmdDescribe()
        {
            string s = EnvDescriber.Describe(transform);
            AccessibilityManager.Instance?.SpeakNow(s);
        }

        public float HeadingRad => _heading;
    }
}
