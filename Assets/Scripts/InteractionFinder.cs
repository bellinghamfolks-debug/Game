using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Sits on the player. Picks the best target when the player taps:
    /// 1. A navigation target if active.
    /// 2. The nearest DOOR within 2.5m (doors are sticky — the player
    ///    almost always wants to enter the door beside them).
    /// 3. The geometrically nearest interactable within 3.5m.
    /// Always speaks a result, never silent.
    /// </summary>
    public class InteractionFinder : MonoBehaviour
    {
        public float searchRadius = 3.5f;
        public float doorPreferRadius = 2.5f;

        Collider[] _buf = new Collider[16];

        public void TryInteract()
        {
            // 1. nav target
            var nav = NavGuide.Instance;
            if (nav != null && nav.HasTarget)
            {
                float d = Vector3.Distance(transform.position, nav.TargetPosition);
                if (d < searchRadius + 1.0f)
                {
                    Resolve(nav.TargetInteractable);
                    nav.Cancel();
                    return;
                }
            }

            // 2 & 3. scan area
            int n = Physics.OverlapSphereNonAlloc(transform.position, searchRadius, _buf);
            Interactable bestDoor = null, bestAny = null;
            float bestDoorD = float.MaxValue, bestAnyD = float.MaxValue;
            for (int i = 0; i < n; i++)
            {
                var col = _buf[i];
                if (col == null) continue;
                var it = col.GetComponentInParent<Interactable>();
                if (it == null) continue;
                float d = Vector3.Distance(transform.position, col.transform.position);
                if (it.kind == InteractableKind.Door)
                {
                    if (d < bestDoorD) { bestDoor = it; bestDoorD = d; }
                }
                if (d < bestAnyD) { bestAny = it; bestAnyD = d; }
            }
            if (bestDoor != null && bestDoorD < doorPreferRadius) { Resolve(bestDoor); return; }
            if (bestAny != null) { Resolve(bestAny); return; }

            // 4. nothing close — give helpful feedback
            float scanRange = 8f;
            int wn = Physics.OverlapSphereNonAlloc(transform.position, scanRange, _buf);
            Interactable nearest = null;
            float nearestD = float.MaxValue;
            for (int i = 0; i < wn; i++)
            {
                var it = _buf[i] != null ? _buf[i].GetComponentInParent<Interactable>() : null;
                if (it == null) continue;
                float d = Vector3.Distance(transform.position, _buf[i].transform.position);
                if (d < nearestD) { nearest = it; nearestD = d; }
            }
            if (nearest != null)
            {
                int steps = Mathf.Max(1, Mathf.RoundToInt(nearestD));
                AccessibilityManager.Instance?.SpeakNow(
                    "لا شيء قريب جدًا للتفاعل. " + nearest.displayName + " على بُعد " + steps + " خطوة. اقترب أكثر.");
            }
            else AccessibilityManager.Instance?.SpeakNow("لا يوجد شيء حولك للتفاعل معه.");
        }

        void Resolve(Interactable it)
        {
            if (it == null) return;
            // Mission flag + memory FIRST so the OnInteract hook fires before TTS.
            GameManager.Instance?.OnInteract(it);
            switch (it.kind)
            {
                case InteractableKind.Door:
                    AccessibilityManager.Instance?.SpeakNow(
                        "تفتح " + it.displayName + ".");
                    AccessibilityManager.Instance?.Confirm();
                    break;
                case InteractableKind.Person:
                    DialogueSystem.StartConversation(it);
                    break;
                default:
                    AccessibilityManager.Instance?.SpeakNow(
                        it.displayName + ". " + (it.description ?? ""));
                    AccessibilityManager.Instance?.Confirm();
                    break;
            }
        }
    }
}
