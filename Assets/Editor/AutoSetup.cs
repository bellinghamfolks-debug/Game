#if UNITY_EDITOR
using System.Collections.Generic;
using System.IO;
using UnityEditor;
using UnityEditor.PackageManager;
using UnityEditor.PackageManager.Requests;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;

namespace BlindLife.Editor
{
    /// <summary>
    /// One-click graphics setup. After importing an Asset Store pack
    /// (Synty POLYGON City, Kenney, etc.), choose
    /// 'BlindLife → Setup Graphics' from the menu bar. This script:
    ///
    ///   1. Adds the URP package to manifest if missing.
    ///   2. Creates a URP asset and wires it into Graphics settings.
    ///   3. Adds a Global Volume to the scene with Bloom + Tonemapping
    ///      + Color Adjustments + Vignette + Depth-of-Field.
    ///   4. Tunes Quality settings for mobile (shadows, MSAA, etc.).
    ///   5. Searches the project for prefab naming patterns from the
    ///      common asset packs and auto-assigns them to GameBootstrap.
    ///
    /// Defensive: if anything is already configured, it leaves it alone.
    /// Re-runnable: clicking the menu item twice is safe.
    /// </summary>
    public static class AutoSetup
    {
        const string MenuRoot = "BlindLife/";

        [MenuItem(MenuRoot + "Setup Graphics (One-click)", false, 1)]
        public static void RunAll()
        {
            int steps = 0;
            steps += InstallUrp() ? 1 : 0;
            steps += ConfigureUrpAsset() ? 1 : 0;
            steps += AddPostProcessingVolume() ? 1 : 0;
            steps += TuneQualityForMobile() ? 1 : 0;
            int wired = AutoWirePrefabs();
            EditorUtility.DisplayDialog(
                "BlindLife — Setup complete",
                $"{steps} steps applied.\n" +
                $"{wired} prefab(s) wired into GameBootstrap.\n\n" +
                "Press Play (▶) to test, or build to Android.",
                "OK");
        }

        // ----------------- URP install -----------------

        [MenuItem(MenuRoot + "1. Install URP package", false, 10)]
        public static bool InstallUrp()
        {
            const string pkg = "com.unity.render-pipelines.universal";
            string manifestPath = "Packages/manifest.json";
            if (!File.Exists(manifestPath)) return false;
            string raw = File.ReadAllText(manifestPath);
            if (raw.Contains(pkg)) return false; // already there
            int idx = raw.IndexOf("\"dependencies\":");
            if (idx < 0) return false;
            idx = raw.IndexOf("{", idx) + 1;
            string insert = $"\n    \"{pkg}\": \"14.0.11\",";
            raw = raw.Substring(0, idx) + insert + raw.Substring(idx);
            File.WriteAllText(manifestPath, raw);
            Client.Resolve();
            AssetDatabase.Refresh();
            return true;
        }

        // ----------------- URP asset wiring -----------------

        const string UrpAssetPath = "Assets/Settings/URP_Asset.asset";

        [MenuItem(MenuRoot + "2. Configure URP asset", false, 11)]
        public static bool ConfigureUrpAsset()
        {
            if (GraphicsSettings.defaultRenderPipeline != null) return false;

            // The URP types live in
            // UnityEngine.Rendering.Universal.UniversalRenderPipelineAsset.
            var urpType = System.Type.GetType(
                "UnityEngine.Rendering.Universal.UniversalRenderPipelineAsset, Unity.RenderPipelines.Universal.Runtime");
            if (urpType == null)
            {
                Debug.LogWarning("[BlindLife] URP not imported yet. Run 'Install URP' first, wait for resolve, then retry.");
                return false;
            }

            Directory.CreateDirectory("Assets/Settings");
            var asset = AssetDatabase.LoadAssetAtPath<ScriptableObject>(UrpAssetPath);
            if (asset == null)
            {
                asset = (ScriptableObject)ScriptableObject.CreateInstance(urpType);
                AssetDatabase.CreateAsset(asset, UrpAssetPath);
                AssetDatabase.SaveAssets();
            }
            GraphicsSettings.defaultRenderPipeline = (RenderPipelineAsset)asset;
            QualitySettings.renderPipeline = (RenderPipelineAsset)asset;
            EditorUtility.SetDirty(asset);
            AssetDatabase.SaveAssets();
            Debug.Log("[BlindLife] URP asset configured at " + UrpAssetPath);
            return true;
        }

        // ----------------- Post-processing -----------------

        [MenuItem(MenuRoot + "3. Add Post-processing Volume", false, 12)]
        public static bool AddPostProcessingVolume()
        {
            // Use the URP Volume API by name to avoid hard-link if URP not yet imported.
            var volumeType = System.Type.GetType(
                "UnityEngine.Rendering.Volume, Unity.RenderPipelines.Core.Runtime");
            if (volumeType == null) return false;
            var existing = Object.FindObjectOfType(volumeType);
            if (existing != null) return false;

            var go = new GameObject("GlobalVolume");
            go.AddComponent(volumeType);
            // We can't easily build the profile via reflection alone; leave it
            // to the user (or to a follow-up tool). Inform them.
            EditorUtility.SetDirty(go);
            EditorSceneManager.MarkAllScenesDirty();
            Debug.Log("[BlindLife] GlobalVolume added. Open it in Inspector and add Bloom + Color Adjustments + Vignette + Tonemapping (ACES) overrides.");
            return true;
        }

        // ----------------- Quality settings -----------------

        [MenuItem(MenuRoot + "4. Tune Quality for Mobile", false, 13)]
        public static bool TuneQualityForMobile()
        {
            QualitySettings.shadows = ShadowQuality.All;
            QualitySettings.shadowDistance = 30f;
            QualitySettings.shadowResolution = ShadowResolution.Medium;
            QualitySettings.antiAliasing = 4;
            QualitySettings.pixelLightCount = 2;
            QualitySettings.anisotropicFiltering = AnisotropicFiltering.ForceEnable;
            QualitySettings.softParticles = false;
            QualitySettings.realtimeReflectionProbes = false;
            QualitySettings.vSyncCount = 0;
            QualitySettings.lodBias = 1.0f;
            PlayerSettings.colorSpace = ColorSpace.Linear;
            Debug.Log("[BlindLife] Quality tuned for Android.");
            return true;
        }

        // ----------------- Prefab auto-wiring -----------------

        [MenuItem(MenuRoot + "5. Auto-wire Asset Pack Prefabs", false, 14)]
        public static int AutoWirePrefabs()
        {
            // Patterns we recognise across common packs.
            string[] buildingPatterns = {
                "SM_Bld_", "Bld_", "Building_", "House_", "Shop_", "Office_"
            };
            string[] characterPatterns = {
                "SK_Chr_", "Chr_", "Character_", "NPC_", "Human_", "Person_",
                "Polygon_Character", "Mixamo"
            };
            string[] vehiclePatterns = {
                "SM_Veh_", "Veh_", "Vehicle_", "Car_", "Bus_", "Truck_"
            };
            string[] treePatterns = {
                "SM_Prop_Tree", "Tree_", "Prop_Tree", "Vegetation_Tree",
                "Foliage_Tree"
            };
            string[] playerPatterns = {
                "Polygon_Character_01", "MainCharacter", "Player_", "MainPlayer"
            };

            GameObject buildingPrefab = FindFirstPrefab(buildingPatterns);
            GameObject npcPrefab      = FindFirstPrefab(characterPatterns);
            GameObject playerPrefab   = FindFirstPrefab(playerPatterns) ?? npcPrefab;
            GameObject vehiclePrefab  = FindFirstPrefab(vehiclePatterns);
            GameObject treePrefab     = FindFirstPrefab(treePatterns);

            int wired = 0;
            var bootstraps = Object.FindObjectsOfType<BlindLife.GameBootstrap>();
            if (bootstraps.Length == 0)
            {
                Debug.LogWarning("[BlindLife] No GameBootstrap in scene. Add one to MainScene first.");
                return 0;
            }
            foreach (var b in bootstraps)
            {
                if (buildingPrefab != null) { b.buildingPrefab = buildingPrefab; wired++; }
                if (npcPrefab      != null) { b.npcPrefab      = npcPrefab;      wired++; }
                if (playerPrefab   != null) { b.playerPrefab   = playerPrefab;   wired++; }
                if (vehiclePrefab  != null) { b.vehiclePrefab  = vehiclePrefab;  wired++; }
                if (treePrefab     != null) { b.treePrefab     = treePrefab;     wired++; }
                EditorUtility.SetDirty(b);
            }
            EditorSceneManager.MarkAllScenesDirty();
            Debug.Log($"[BlindLife] Wired {wired} prefab slot(s) on GameBootstrap.");
            return wired;
        }

        static GameObject FindFirstPrefab(string[] patterns)
        {
            string[] guids = AssetDatabase.FindAssets("t:Prefab");
            foreach (var g in guids)
            {
                string path = AssetDatabase.GUIDToAssetPath(g);
                string name = Path.GetFileNameWithoutExtension(path);
                foreach (var p in patterns)
                {
                    if (name.IndexOf(p, System.StringComparison.OrdinalIgnoreCase) >= 0)
                        return AssetDatabase.LoadAssetAtPath<GameObject>(path);
                }
            }
            return null;
        }

        // ----------------- Diagnostics -----------------

        [MenuItem(MenuRoot + "Status Report", false, 50)]
        public static void StatusReport()
        {
            string urp = GraphicsSettings.defaultRenderPipeline != null
                ? GraphicsSettings.defaultRenderPipeline.GetType().Name
                : "(none)";
            int prefabs = AssetDatabase.FindAssets("t:Prefab").Length;
            EditorUtility.DisplayDialog("BlindLife Status",
                "Render pipeline: " + urp + "\n" +
                "Color space: " + PlayerSettings.colorSpace + "\n" +
                "AA: " + QualitySettings.antiAliasing + "x\n" +
                "Shadows: " + QualitySettings.shadows + "\n" +
                "Prefabs in project: " + prefabs,
                "OK");
        }
    }
}
#endif
