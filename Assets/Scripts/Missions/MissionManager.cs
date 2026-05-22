using System.Collections.Generic;
using UnityEngine;

namespace BlindLife.Missions
{
    public class MissionManager
    {
        public delegate void MissionCompletedHandler(Mission previous, Mission next);
        public delegate void AllCompletedHandler();
        public event MissionCompletedHandler OnMissionCompleted;
        public event AllCompletedHandler OnAllCompleted;

        readonly List<Mission> _all;
        int _idx;

        public MissionManager(List<Mission> missions)
        {
            _all = missions ?? new List<Mission>();
        }

        public Mission Current => _idx < _all.Count ? _all[_idx] : null;
        public int Progress => _idx;
        public int Total => _all.Count;
        public IReadOnlyList<Mission> All => _all;
        public bool AllCompleted => _idx >= _all.Count;

        public void Prime(GameState gs)
        {
            Current?.OnStart(gs);
        }

        public void Check(GameState gs)
        {
            var cur = Current;
            if (cur == null || cur.completed) return;
            if (!cur.Check(gs)) return;

            cur.completed = true;
            cur.OnComplete(gs);
            _idx++;
            var next = Current;
            next?.OnStart(gs);
            OnMissionCompleted?.Invoke(cur, next);
            if (next == null) OnAllCompleted?.Invoke();
        }

        public string Save() => _idx.ToString();
        public void Load(string s)
        {
            if (string.IsNullOrEmpty(s)) return;
            if (int.TryParse(s, out var n))
            {
                _idx = Mathf.Clamp(n, 0, _all.Count);
                for (int i = 0; i < _idx; i++) _all[i].completed = true;
            }
        }
    }
}
