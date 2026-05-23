#if UNITY_EDITOR
using System.IO;
using System.IO.Compression;
using UnityEditor;
using UnityEngine;

namespace BlindLife.Editor
{
    /// <summary>
    /// Helpers for the free Kenney 3D asset packs. Provides direct
    /// browser links to the download pages on kenney.nl and a
    /// 'Import ZIP into Assets/Kenney' utility so the user can drop a
    /// downloaded archive into the project without manual extraction.
    ///
    /// Why not auto-download? kenney.nl uses content-hashed URLs that
    /// rotate, so there's no stable direct-link. Opening the page in
    /// the browser is the most reliable path.
    /// </summary>
    public static class KenneyHelper
    {
        const string Root = "BlindLife/Kenney/";

        [MenuItem(Root + "1. Open: City Kit (Commercial) [buildings]", false, 0)]
        public static void OpenCity() => Application.OpenURL("https://kenney.nl/assets/city-kit-commercial");

        [MenuItem(Root + "2. Open: City Kit (Suburban) [houses]", false, 1)]
        public static void OpenSuburb() => Application.OpenURL("https://kenney.nl/assets/city-kit-suburban");

        [MenuItem(Root + "3. Open: City Kit (Roads)", false, 2)]
        public static void OpenRoads() => Application.OpenURL("https://kenney.nl/assets/city-kit-roads");

        [MenuItem(Root + "4. Open: Car Kit", false, 3)]
        public static void OpenCars() => Application.OpenURL("https://kenney.nl/assets/car-kit");

        [MenuItem(Root + "5. Open: Nature Kit [trees]", false, 4)]
        public static void OpenNature() => Application.OpenURL("https://kenney.nl/assets/nature-kit");

        [MenuItem(Root + "6. Open: Mini Characters 1 [NPCs + player]", false, 5)]
        public static void OpenMini() => Application.OpenURL("https://kenney.nl/assets/mini-characters-1");

        [MenuItem(Root + "7. Open: Toon Characters", false, 6)]
        public static void OpenToon() => Application.OpenURL("https://kenney.nl/assets/toon-characters");

        [MenuItem(Root + "Import a downloaded ZIP into Assets/Kenney…", false, 20)]
        public static void ImportZip()
        {
            string zipPath = EditorUtility.OpenFilePanel(
                "Pick a Kenney ZIP file you downloaded", "", "zip");
            if (string.IsNullOrEmpty(zipPath) || !File.Exists(zipPath))
            {
                Debug.Log("[Kenney] No ZIP selected.");
                return;
            }
            string targetRoot = Path.Combine(Application.dataPath, "Kenney");
            Directory.CreateDirectory(targetRoot);

            // Extract into a sub-folder named after the zip.
            string subName = Path.GetFileNameWithoutExtension(zipPath);
            string target = Path.Combine(targetRoot, subName);
            if (Directory.Exists(target))
            {
                if (!EditorUtility.DisplayDialog(
                        "Already imported",
                        target + "\nexists. Overwrite?", "Overwrite", "Cancel"))
                    return;
                Directory.Delete(target, true);
            }
            try
            {
                using (var archive = ZipFile.OpenRead(zipPath))
                {
                    foreach (var entry in archive.Entries)
                    {
                        if (string.IsNullOrEmpty(entry.Name)) continue; // dir entry
                        string outPath = Path.Combine(target, entry.FullName);
                        Directory.CreateDirectory(Path.GetDirectoryName(outPath));
                        entry.ExtractToFile(outPath, overwrite: true);
                    }
                }
                AssetDatabase.Refresh();
                Debug.Log("[Kenney] Imported into Assets/Kenney/" + subName);
                EditorUtility.DisplayDialog("Kenney",
                    "Extracted " + subName + " into Assets/Kenney/.\n\n" +
                    "Now click: BlindLife → Setup Graphics", "OK");
            }
            catch (System.Exception e)
            {
                Debug.LogError("[Kenney] Extract failed: " + e);
                EditorUtility.DisplayDialog("Kenney",
                    "Could not extract ZIP:\n" + e.Message, "OK");
            }
        }

        [MenuItem(Root + "Open downloads page (all Kenney packs)", false, 30)]
        public static void OpenAll() => Application.OpenURL("https://kenney.nl/assets?q=3d");
    }
}
#endif
