#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace BlindLife.Editor
{
    /// <summary>
    /// Called by CI via -executeMethod BlindLife.Editor.BuildScript.AndroidBuild.
    /// Writes an APK to build/Android/BlindLife.apk.
    /// </summary>
    public static class BuildScript
    {
        [MenuItem("Build/Android APK")]
        public static void AndroidBuild()
        {
            var scenes = new[] { "Assets/Scenes/MainScene.unity" };
            PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARMv7 | AndroidArchitecture.ARM64;
            EditorUserBuildSettings.buildAppBundle = false;

            var options = new BuildPlayerOptions
            {
                scenes = scenes,
                locationPathName = "build/Android/BlindLife.apk",
                target = BuildTarget.Android,
                targetGroup = BuildTargetGroup.Android,
                options = BuildOptions.None
            };
            BuildReport report = BuildPipeline.BuildPlayer(options);
            BuildSummary summary = report.summary;
            if (summary.result == BuildResult.Succeeded)
            {
                Debug.Log("APK build succeeded: " + summary.totalSize + " bytes");
                EditorApplication.Exit(0);
            }
            else
            {
                Debug.LogError("APK build failed: " + summary.result);
                EditorApplication.Exit(1);
            }
        }
    }
}
#endif
