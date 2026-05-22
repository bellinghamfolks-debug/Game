using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Light-weight save / load. Serialises GameState into PlayerPrefs as
    /// JSON. CharacterMemory is stored independently by its own Save/Load.
    /// </summary>
    public static class SaveManager
    {
        const string Key = "blindlife_state_v1";

        [System.Serializable]
        class Save
        {
            public int day = 1;
            public int minutes;
            public int missionIdx;
            public int mobility = 30, social = 30, tech = 20, confidence = 25;
            public float playerX, playerY, playerZ;
            public float headingRad;
            public string[] flags;
        }

        public static bool HasSave => PlayerPrefs.HasKey(Key);

        public static void Save(GameState gs, Vector3 playerPos, float headingRad)
        {
            if (gs == null) return;
            var s = new Save
            {
                day = gs.day,
                minutes = gs.minutes,
                missionIdx = gs.missionIdx,
                mobility = gs.mobility,
                social = gs.social,
                tech = gs.tech,
                confidence = gs.confidence,
                playerX = playerPos.x,
                playerY = playerPos.y,
                playerZ = playerPos.z,
                headingRad = headingRad,
                flags = new string[gs.flags.Count]
            };
            int i = 0;
            foreach (var f in gs.flags) s.flags[i++] = f;
            PlayerPrefs.SetString(Key, JsonUtility.ToJson(s));
            PlayerPrefs.Save();
        }

        public static void Load(GameState gs, out Vector3 playerPos, out float headingRad)
        {
            playerPos = Vector3.zero;
            headingRad = 0;
            if (gs == null || !HasSave) return;
            string raw = PlayerPrefs.GetString(Key, "");
            if (string.IsNullOrEmpty(raw)) return;
            try
            {
                var s = JsonUtility.FromJson<Save>(raw);
                if (s == null) return;
                gs.day = s.day;
                gs.minutes = s.minutes;
                gs.missionIdx = s.missionIdx;
                gs.mobility = s.mobility;
                gs.social = s.social;
                gs.tech = s.tech;
                gs.confidence = s.confidence;
                gs.flags.Clear();
                if (s.flags != null) foreach (var f in s.flags) gs.flags.Add(f);
                playerPos = new Vector3(s.playerX, s.playerY, s.playerZ);
                headingRad = s.headingRad;
            }
            catch (System.Exception e) { Debug.LogWarning("save load: " + e); }
        }

        public static void Clear()
        {
            PlayerPrefs.DeleteKey(Key);
            PlayerPrefs.Save();
        }
    }
}
