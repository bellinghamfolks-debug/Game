using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Smooth third-person follow with a subtle walk bob.
    /// Replaceable with Cinemachine when the user adds it from the package manager.
    /// </summary>
    public class CameraFollow : MonoBehaviour
    {
        public Transform target;
        public Vector3 offset = new Vector3(0, 5f, -6f);
        public float followSpeed = 4f;
        public float lookHeight = 1.5f;

        Vector3 _vel;

        void LateUpdate()
        {
            if (target == null) return;
            Vector3 wanted = target.position + target.rotation * offset;
            transform.position = Vector3.SmoothDamp(transform.position, wanted, ref _vel, 1f / followSpeed);
            transform.LookAt(target.position + Vector3.up * lookHeight);
        }
    }
}
