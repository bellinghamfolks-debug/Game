using UnityEngine;

namespace BlindLife.World
{
    /// <summary>
    /// Tiny per-NPC AI: pick a random destination within a radius around
    /// the spawn point, walk toward it, idle, repeat. Gives the world a
    /// feeling of being inhabited even without proper animation rigs.
    /// </summary>
    public class NpcWanderer : MonoBehaviour
    {
        public float radius = 3.5f;
        public float speed = 0.7f;
        public float idleMin = 2f, idleMax = 6f;

        Vector3 _home;
        Vector3 _dest;
        float _idleUntil;
        bool _walking;
        float _bobPhase;

        void Start()
        {
            _home = transform.position;
            PickNewDestination();
        }

        void Update()
        {
            if (_walking)
            {
                Vector3 toDest = _dest - transform.position;
                toDest.y = 0;
                if (toDest.sqrMagnitude < 0.05f)
                {
                    _walking = false;
                    _idleUntil = Time.time + Random.Range(idleMin, idleMax);
                }
                else
                {
                    Vector3 step = toDest.normalized * speed * Time.deltaTime;
                    transform.position += step;
                    if (toDest.sqrMagnitude > 0.001f)
                    {
                        var look = Quaternion.LookRotation(toDest.normalized, Vector3.up);
                        transform.rotation = Quaternion.Slerp(transform.rotation, look, 4f * Time.deltaTime);
                    }
                    _bobPhase += Time.deltaTime * 8f;
                    // Body bob handled by child renderer if any — skip if no specific bone.
                }
            }
            else
            {
                if (Time.time >= _idleUntil) PickNewDestination();
            }
        }

        void PickNewDestination()
        {
            Vector2 r = Random.insideUnitCircle * radius;
            _dest = _home + new Vector3(r.x, 0, r.y);
            _walking = true;
        }
    }
}
