using UnityEngine;

namespace BlindLife.World
{
    /// <summary>
    /// Adds street furniture and ground markings purely for visual depth:
    /// lamp posts, benches, traffic cones, manhole covers, road dashes,
    /// crosswalk stripes. None of these are interactable; they just make
    /// the city feel populated.
    /// </summary>
    public class WorldDecorations : MonoBehaviour
    {
        public static void Build(Transform parent)
        {
            BuildLampPosts(parent);
            BuildBenches(parent);
            BuildTrafficCones(parent);
            BuildManholes(parent);
            BuildRoadMarkings(parent);
            BuildCrosswalks(parent);
            BuildHydrants(parent);
            BuildSigns(parent);
        }

        static Material Mat(Color c, float gloss = 0.2f)
        {
            var m = new Material(Shader.Find("Standard"));
            m.color = c;
            m.SetFloat("_Glossiness", gloss);
            return m;
        }

        static void BuildLampPosts(Transform parent)
        {
            // 12 lamp posts along the cross-road sidewalks
            float[] xs = { -22, -14, -6, 6, 14, 22 };
            foreach (float x in xs)
            {
                CreateLampPost(parent, new Vector3(x, 0, -3.4f));
                CreateLampPost(parent, new Vector3(x, 0,  3.4f));
            }
            float[] zs = { -22, -14, -6, 6, 14, 22 };
            foreach (float z in zs)
            {
                CreateLampPost(parent, new Vector3(-3.4f, 0, z));
                CreateLampPost(parent, new Vector3( 3.4f, 0, z));
            }
        }

        static void CreateLampPost(Transform parent, Vector3 pos)
        {
            var post = new GameObject("LampPost");
            post.transform.SetParent(parent);
            post.transform.position = pos;
            // pole
            var pole = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            pole.transform.SetParent(post.transform);
            pole.transform.localPosition = Vector3.up * 2.4f;
            pole.transform.localScale = new Vector3(0.12f, 2.4f, 0.12f);
            pole.GetComponent<Renderer>().material = Mat(new Color(0.18f, 0.18f, 0.2f), 0.5f);
            // arm
            var arm = GameObject.CreatePrimitive(PrimitiveType.Cube);
            arm.transform.SetParent(post.transform);
            arm.transform.localPosition = new Vector3(0.3f, 4.6f, 0);
            arm.transform.localScale = new Vector3(0.65f, 0.06f, 0.06f);
            arm.GetComponent<Renderer>().material = pole.GetComponent<Renderer>().material;
            // head
            var head = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            head.transform.SetParent(post.transform);
            head.transform.localPosition = new Vector3(0.55f, 4.5f, 0);
            head.transform.localScale = Vector3.one * 0.3f;
            var hm = Mat(new Color(1f, 0.92f, 0.65f), 0.8f);
            hm.EnableKeyword("_EMISSION");
            hm.SetColor("_EmissionColor", new Color(1f, 0.9f, 0.5f) * 0.6f);
            head.GetComponent<Renderer>().material = hm;
            // small bulb light
            var lightGo = new GameObject("LampLight");
            lightGo.transform.SetParent(post.transform);
            lightGo.transform.localPosition = new Vector3(0.55f, 4.3f, 0);
            var pt = lightGo.AddComponent<Light>();
            pt.type = LightType.Point;
            pt.range = 6f;
            pt.intensity = 1.0f;
            pt.color = new Color(1f, 0.85f, 0.55f);
        }

        static void BuildBenches(Transform parent)
        {
            CreateBench(parent, new Vector3(-10, 0, -5.5f));
            CreateBench(parent, new Vector3( 10, 0, -5.5f));
            CreateBench(parent, new Vector3(-10, 0,  5.5f));
            CreateBench(parent, new Vector3( 10, 0,  5.5f));
        }
        static void CreateBench(Transform parent, Vector3 pos)
        {
            var b = new GameObject("Bench");
            b.transform.SetParent(parent);
            b.transform.position = pos;
            var seat = GameObject.CreatePrimitive(PrimitiveType.Cube);
            seat.transform.SetParent(b.transform);
            seat.transform.localPosition = Vector3.up * 0.45f;
            seat.transform.localScale = new Vector3(1.8f, 0.1f, 0.45f);
            seat.GetComponent<Renderer>().material = Mat(new Color(0.40f, 0.27f, 0.16f), 0.3f);
            var back = GameObject.CreatePrimitive(PrimitiveType.Cube);
            back.transform.SetParent(b.transform);
            back.transform.localPosition = new Vector3(0, 0.85f, -0.2f);
            back.transform.localScale = new Vector3(1.8f, 0.7f, 0.08f);
            back.GetComponent<Renderer>().material = seat.GetComponent<Renderer>().material;
            for (int i = -1; i <= 1; i += 2)
            {
                var leg = GameObject.CreatePrimitive(PrimitiveType.Cube);
                leg.transform.SetParent(b.transform);
                leg.transform.localPosition = new Vector3(0.7f * i, 0.2f, 0);
                leg.transform.localScale = new Vector3(0.08f, 0.45f, 0.4f);
                leg.GetComponent<Renderer>().material = Mat(new Color(0.18f, 0.18f, 0.2f), 0.5f);
            }
        }

        static void BuildTrafficCones(Transform parent)
        {
            for (int i = 0; i < 8; i++)
            {
                var c = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                c.name = "Cone";
                c.transform.SetParent(parent);
                c.transform.position = new Vector3(Random.Range(-2.5f, 2.5f), 0.3f, Random.Range(-25f, 25f));
                c.transform.localScale = new Vector3(0.35f, 0.3f, 0.35f);
                c.GetComponent<Renderer>().material = Mat(new Color(0.95f, 0.4f, 0.15f), 0.4f);
                Object.Destroy(c.GetComponent<Collider>());
            }
        }

        static void BuildManholes(Transform parent)
        {
            for (int i = 0; i < 4; i++)
            {
                var m = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                m.name = "Manhole";
                m.transform.SetParent(parent);
                m.transform.position = new Vector3(Random.Range(-2.5f, 2.5f), 0.04f, Random.Range(-25f, 25f));
                m.transform.localScale = new Vector3(0.7f, 0.02f, 0.7f);
                m.GetComponent<Renderer>().material = Mat(new Color(0.15f, 0.15f, 0.17f), 0.7f);
                Object.Destroy(m.GetComponent<Collider>());
            }
        }

        static void BuildRoadMarkings(Transform parent)
        {
            // Animated road dashes along both road strips.
            // (They're static here; the cycle is purely cosmetic.)
            Material yellow = Mat(new Color(0.95f, 0.78f, 0.2f), 0.5f);
            // Horizontal road dashes
            for (float x = -28; x <= 28; x += 2.4f)
            {
                var d = GameObject.CreatePrimitive(PrimitiveType.Cube);
                d.name = "RoadDash";
                d.transform.SetParent(parent);
                d.transform.position = new Vector3(x, 0.05f, 0);
                d.transform.localScale = new Vector3(1.2f, 0.05f, 0.18f);
                d.GetComponent<Renderer>().material = yellow;
                Object.Destroy(d.GetComponent<Collider>());
            }
            // Vertical road dashes
            for (float z = -28; z <= 28; z += 2.4f)
            {
                var d = GameObject.CreatePrimitive(PrimitiveType.Cube);
                d.name = "RoadDash";
                d.transform.SetParent(parent);
                d.transform.position = new Vector3(0, 0.05f, z);
                d.transform.localScale = new Vector3(0.18f, 0.05f, 1.2f);
                d.GetComponent<Renderer>().material = yellow;
                Object.Destroy(d.GetComponent<Collider>());
            }
        }

        static void BuildCrosswalks(Transform parent)
        {
            Material white = Mat(new Color(0.95f, 0.95f, 0.95f), 0.4f);
            // Four crosswalks at the intersection
            for (int side = 0; side < 4; side++)
            {
                bool horizontal = side < 2;
                int sign = (side % 2 == 0) ? 1 : -1;
                for (int s = -3; s <= 3; s++)
                {
                    var stripe = GameObject.CreatePrimitive(PrimitiveType.Cube);
                    stripe.name = "Crosswalk";
                    stripe.transform.SetParent(parent);
                    if (horizontal)
                    {
                        stripe.transform.position = new Vector3(sign * 4.2f, 0.06f, s * 0.5f);
                        stripe.transform.localScale = new Vector3(2.0f, 0.06f, 0.30f);
                    }
                    else
                    {
                        stripe.transform.position = new Vector3(s * 0.5f, 0.06f, sign * 4.2f);
                        stripe.transform.localScale = new Vector3(0.30f, 0.06f, 2.0f);
                    }
                    stripe.GetComponent<Renderer>().material = white;
                    Object.Destroy(stripe.GetComponent<Collider>());
                }
            }
        }

        static void BuildHydrants(Transform parent)
        {
            CreateHydrant(parent, new Vector3(-6, 0, -3.6f));
            CreateHydrant(parent, new Vector3( 6, 0,  3.6f));
        }
        static void CreateHydrant(Transform parent, Vector3 pos)
        {
            var h = new GameObject("Hydrant");
            h.transform.SetParent(parent);
            h.transform.position = pos;
            var b = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            b.transform.SetParent(h.transform);
            b.transform.localPosition = Vector3.up * 0.4f;
            b.transform.localScale = new Vector3(0.3f, 0.4f, 0.3f);
            b.GetComponent<Renderer>().material = Mat(new Color(0.85f, 0.15f, 0.15f), 0.6f);
            var top = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            top.transform.SetParent(h.transform);
            top.transform.localPosition = Vector3.up * 0.9f;
            top.transform.localScale = Vector3.one * 0.35f;
            top.GetComponent<Renderer>().material = b.GetComponent<Renderer>().material;
        }

        static void BuildSigns(Transform parent)
        {
            CreateSign(parent, new Vector3(-3.6f, 0, -22), "→ مدينة");
            CreateSign(parent, new Vector3( 3.6f, 0,  22), "→ مدينة");
            CreateSign(parent, new Vector3(-22, 0,  3.6f), "← مدينة");
            CreateSign(parent, new Vector3( 22, 0, -3.6f), "→ مدينة");
        }
        static void CreateSign(Transform parent, Vector3 pos, string label)
        {
            var sign = new GameObject("Sign_" + label);
            sign.transform.SetParent(parent);
            sign.transform.position = pos;
            var pole = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            pole.transform.SetParent(sign.transform);
            pole.transform.localPosition = Vector3.up * 1.0f;
            pole.transform.localScale = new Vector3(0.08f, 1.0f, 0.08f);
            pole.GetComponent<Renderer>().material = Mat(new Color(0.5f, 0.5f, 0.55f), 0.5f);
            var board = GameObject.CreatePrimitive(PrimitiveType.Cube);
            board.transform.SetParent(sign.transform);
            board.transform.localPosition = Vector3.up * 2.1f;
            board.transform.localScale = new Vector3(0.9f, 0.45f, 0.05f);
            board.GetComponent<Renderer>().material = Mat(new Color(0.25f, 0.45f, 0.75f), 0.6f);
        }
    }
}
