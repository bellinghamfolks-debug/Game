using UnityEngine;

namespace BlindLife.World
{
    /// <summary>
    /// Cheap walk-bob: the visual body slightly bobs up + tilts forward
    /// while the rigidbody is moving. Survives an Asset-Store swap because
    /// it operates on the root transform of the visible child.
    /// </summary>
    public class PlayerWalkBob : MonoBehaviour
    {
        public Transform body;
        public float bobFreq = 9f;
        public float bobAmp = 0.05f;
        public float tiltAmp = 4f;

        Rigidbody _rb;
        float _phase;
        Vector3 _baseLocal;
        Quaternion _baseRot;

        void Start()
        {
            _rb = GetComponentInParent<Rigidbody>();
            if (body == null && transform.childCount > 0) body = transform.GetChild(0);
            if (body == null) body = transform;
            _baseLocal = body.localPosition;
            _baseRot = body.localRotation;
        }

        void Update()
        {
            float speed = (_rb != null) ? _rb.velocity.magnitude : 0f;
            bool moving = speed > 0.2f;
            if (moving)
            {
                _phase += Time.deltaTime * bobFreq;
                float bob = Mathf.Abs(Mathf.Sin(_phase)) * bobAmp;
                float tilt = Mathf.Sin(_phase * 0.5f) * tiltAmp;
                body.localPosition = _baseLocal + Vector3.up * bob;
                body.localRotation = _baseRot * Quaternion.Euler(-tiltAmp * 0.4f, 0, tilt);
            }
            else
            {
                body.localPosition = Vector3.Lerp(body.localPosition, _baseLocal, 6f * Time.deltaTime);
                body.localRotation = Quaternion.Slerp(body.localRotation, _baseRot, 6f * Time.deltaTime);
            }
        }
    }
}
