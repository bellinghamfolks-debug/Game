using UnityEngine;

namespace BlindLife.Missions
{
    /// <summary>
    /// One unit of progress. The Check() predicate is polled every
    /// game tick; when true the mission completes and the next one
    /// starts via MissionManager.
    /// </summary>
    public abstract class Mission
    {
        public readonly string id;
        public readonly string title;
        public readonly string description;
        public readonly string hint;
        public bool completed;

        protected Mission(string id, string title, string description, string hint)
        {
            this.id = id;
            this.title = title;
            this.description = description;
            this.hint = hint;
        }

        public abstract bool Check(GameState gs);
        public virtual void OnStart(GameState gs) {}
        public virtual void OnComplete(GameState gs) {}
    }

    /// <summary>Player must call Interact on a specific entity id.</summary>
    public class InteractMission : Mission
    {
        public readonly string entityId;
        public readonly int rewardMobility, rewardSocial, rewardTech, rewardConfidence;

        public InteractMission(string id, string title, string desc, string hint, string entityId,
            int mob = 0, int soc = 0, int tech = 0, int conf = 1)
            : base(id, title, desc, hint)
        {
            this.entityId = entityId;
            rewardMobility = mob; rewardSocial = soc;
            rewardTech = tech; rewardConfidence = conf;
        }
        public override bool Check(GameState gs) => gs.HasFlag("i:" + entityId);
        public override void OnStart(GameState gs) => gs.ClearFlag("i:" + entityId);
        public override void OnComplete(GameState gs)
        {
            gs.mobility += rewardMobility;
            gs.social += rewardSocial;
            gs.tech += rewardTech;
            gs.confidence += rewardConfidence;
            gs.ClampStats();
        }
    }

    /// <summary>Player must enter a circular region.</summary>
    public class ReachPointMission : Mission
    {
        public readonly Vector3 target;
        public readonly float radius;

        public ReachPointMission(string id, string title, string desc, string hint,
            Vector3 target, float radius)
            : base(id, title, desc, hint)
        {
            this.target = target;
            this.radius = radius;
        }
        public override bool Check(GameState gs)
        {
            if (gs.playerTransform == null) return false;
            return (gs.playerTransform.position - target).sqrMagnitude <= radius * radius;
        }
        public override void OnComplete(GameState gs)
        {
            gs.mobility += 1; gs.ClampStats();
        }
    }
}
