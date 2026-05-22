using UnityEngine;
using UnityEngine.UI;

namespace BlindLife.Vision
{
    public enum VisionMode { Total, Low, Blur, Central, Peripheral, Sighted }

    /// <summary>
    /// Renders a vision-impairment overlay above the world. Procedural —
    /// generates radial-gradient textures at runtime for tunnel / peripheral
    /// vision, and uses tinted full-screen quads for low-vision and blur.
    ///
    /// For more realistic blur use a URP volume with Depth-of-Field; this
    /// implementation runs on the Built-in pipeline so the project builds
    /// without extra packages.
    /// </summary>
    public class VisionFilter : MonoBehaviour
    {
        public VisionMode mode = VisionMode.Sighted;
        Canvas _canvas;
        RawImage _overlay;
        Texture2D _tunnelTex, _peripheralTex;

        void Awake()
        {
            BuildOverlay();
            ApplyMode(mode);
        }

        public void SetMode(VisionMode m)
        {
            mode = m;
            ApplyMode(m);
        }

        void BuildOverlay()
        {
            var go = new GameObject("VisionCanvas");
            go.transform.SetParent(transform);
            _canvas = go.AddComponent<Canvas>();
            _canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            _canvas.sortingOrder = 30;
            go.AddComponent<CanvasScaler>().uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;

            var imgGo = new GameObject("Overlay");
            imgGo.transform.SetParent(go.transform, false);
            _overlay = imgGo.AddComponent<RawImage>();
            var rt = _overlay.rectTransform;
            rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
            rt.offsetMin = Vector2.zero; rt.offsetMax = Vector2.zero;
            _overlay.raycastTarget = false;
            _overlay.color = Color.clear;
        }

        void ApplyMode(VisionMode m)
        {
            if (_overlay == null) return;
            switch (m)
            {
                case VisionMode.Sighted:
                    _overlay.texture = null;
                    _overlay.color = Color.clear;
                    break;
                case VisionMode.Total:
                    _overlay.texture = null;
                    _overlay.color = new Color(0, 0, 0, 1);
                    break;
                case VisionMode.Low:
                    _overlay.texture = null;
                    _overlay.color = new Color(0, 0, 0, 0.55f);
                    break;
                case VisionMode.Blur:
                    _overlay.texture = null;
                    _overlay.color = new Color(0.6f, 0.7f, 0.8f, 0.45f);
                    break;
                case VisionMode.Central:
                    if (_tunnelTex == null) _tunnelTex = BuildTunnel(false);
                    _overlay.texture = _tunnelTex;
                    _overlay.color = Color.white;
                    break;
                case VisionMode.Peripheral:
                    if (_peripheralTex == null) _peripheralTex = BuildTunnel(true);
                    _overlay.texture = _peripheralTex;
                    _overlay.color = Color.white;
                    break;
            }
        }

        Texture2D BuildTunnel(bool inverted)
        {
            int sz = 512;
            var tex = new Texture2D(sz, sz, TextureFormat.RGBA32, false);
            float cx = sz * 0.5f, cy = sz * 0.5f;
            for (int y = 0; y < sz; y++)
            {
                for (int x = 0; x < sz; x++)
                {
                    float dx = (x - cx) / cx;
                    float dy = (y - cy) / cy;
                    float r = Mathf.Sqrt(dx * dx + dy * dy);
                    float a;
                    if (!inverted)
                    {
                        // central: clear small center, opaque edges
                        a = Mathf.SmoothStep(0f, 1f, (r - 0.18f) / 0.55f);
                    }
                    else
                    {
                        // peripheral: opaque center, clear edges
                        a = Mathf.SmoothStep(1f, 0f, (r - 0.10f) / 0.50f);
                    }
                    tex.SetPixel(x, y, new Color(0, 0, 0, Mathf.Clamp01(a)));
                }
            }
            tex.Apply();
            return tex;
        }
    }
}
