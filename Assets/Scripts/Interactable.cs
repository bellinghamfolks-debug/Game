using UnityEngine;

namespace BlindLife
{
    public enum InteractableKind { Person, Door, Vehicle, Tree, Object }

    /// <summary>
    /// Attach to anything the player can tap on. Identified by a stable
    /// id so missions, dialogue and AI memory can refer to it.
    /// </summary>
    public class Interactable : MonoBehaviour
    {
        public InteractableKind kind;
        public string id;
        public string displayName;
        public string description;
        public string destinationSceneId; // when kind == Door, the target scene

        public void Setup(InteractableKind k, string id, string displayName = null, string desc = null)
        {
            this.kind = k;
            this.id = id;
            this.displayName = displayName ?? id;
            this.description = desc ?? "";
        }
    }
}
