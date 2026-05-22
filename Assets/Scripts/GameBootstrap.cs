using UnityEngine;
using UnityEngine.AI;

namespace BlindLife
{
    /// <summary>
    /// Builds the entire starting world procedurally so the scene file
    /// itself is tiny and easy to regenerate. The class self-installs at
    /// startup via [RuntimeInitializeOnLoadMethod], so the MainScene can
    /// be a stub — no asset GUIDs to hand-manage.
    ///
    /// When the user installs proper 3D asset packs from the Asset Store
    /// (Synty / KayKit / a GTA-style city pack), this script becomes the
    /// place to swap primitives for prefab references — every entity has
    /// a clear, named role.
    /// </summary>
    public class GameBootstrap : MonoBehaviour
    {
        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void AutoInstall()
        {
            if (FindObjectOfType<GameBootstrap>() != null) return;
            if (Camera.main == null)
            {
                var camGo = new GameObject("Main Camera");
                camGo.tag = "MainCamera";
                var cam = camGo.AddComponent<Camera>();
                cam.clearFlags = CameraClearFlags.SolidColor;
                cam.backgroundColor = new Color(0.55f, 0.7f, 0.85f);
                camGo.AddComponent<AudioListener>();
            }
            var go = new GameObject("BlindLifeBootstrap");
            go.AddComponent<GameBootstrap>();
            DontDestroyOnLoad(go);
        }

        [Header("World")]
        public int worldSize = 60;          // meters
        public Material groundMaterial;
        public Material roadMaterial;
        public Material buildingMaterial;
        public Material doorMaterial;
        public Material npcMaterial;

        [Header("Optional prefab overrides (drop in from Asset Store)")]
        public GameObject playerPrefab;
        public GameObject buildingPrefab;
        public GameObject npcPrefab;
        public GameObject treePrefab;
        public GameObject vehiclePrefab;

        GameManager _gm;
        Transform _root;

        void Awake()
        {
            _root = new GameObject("World").transform;
            _root.SetParent(transform);
        }

        void Start()
        {
            BuildGround();
            BuildRoads();
            BuildBuildings();
            BuildTrees();
            BuildVehicles();
            BuildNPCs();
            BuildPlayer();
            BuildSky();
            BuildNavMesh();

            _gm = GameManager.Instance ?? gameObject.AddComponent<GameManager>();
            _gm.OnWorldReady();
        }

        void BuildGround()
        {
            var ground = GameObject.CreatePrimitive(PrimitiveType.Plane);
            ground.name = "Ground";
            ground.tag = "Ground";
            ground.transform.SetParent(_root);
            ground.transform.localScale = new Vector3(worldSize / 10f, 1, worldSize / 10f);
            ground.GetComponent<Renderer>().material =
                groundMaterial != null ? groundMaterial : MakeColor(new Color(0.30f, 0.42f, 0.20f));
            ground.AddComponent<NavMeshSurfaceMarker>();
            ground.isStatic = true;
        }

        void BuildRoads()
        {
            // Cross-shaped main road through center.
            CreateRoadStrip(new Vector3(0, 0.02f, 0), new Vector3(worldSize, 0.04f, 6f));
            CreateRoadStrip(new Vector3(0, 0.02f, 0), new Vector3(6f, 0.04f, worldSize));
        }
        void CreateRoadStrip(Vector3 pos, Vector3 size)
        {
            var road = GameObject.CreatePrimitive(PrimitiveType.Cube);
            road.name = "Road";
            road.transform.SetParent(_root);
            road.transform.position = pos;
            road.transform.localScale = size;
            road.GetComponent<Renderer>().material =
                roadMaterial != null ? roadMaterial : MakeColor(new Color(0.18f, 0.18f, 0.21f));
            road.GetComponent<Collider>().enabled = false;
            road.isStatic = true;
        }

        void BuildBuildings()
        {
            // Six placeholder buildings around the cross.
            CreateBuilding("Home",       new Vector3(-15, 0, -15), new Vector3(8, 6, 8));
            CreateBuilding("University", new Vector3( 18, 0, -16), new Vector3(12, 10, 10));
            CreateBuilding("Cafe",       new Vector3(-16, 0,  16), new Vector3(7, 4, 7));
            CreateBuilding("Library",    new Vector3( 18, 0,  18), new Vector3(10, 7, 10));
            CreateBuilding("Shop1",      new Vector3( -4, 0, -22), new Vector3(5, 5, 5));
            CreateBuilding("Shop2",      new Vector3(  6, 0, -22), new Vector3(5, 5, 5));
        }
        void CreateBuilding(string id, Vector3 pos, Vector3 size)
        {
            GameObject b;
            if (buildingPrefab != null)
                b = Instantiate(buildingPrefab, pos, Quaternion.identity, _root);
            else
            {
                b = GameObject.CreatePrimitive(PrimitiveType.Cube);
                b.transform.SetParent(_root);
                b.transform.position = pos + Vector3.up * size.y * 0.5f;
                b.transform.localScale = size;
                b.GetComponent<Renderer>().material =
                    buildingMaterial != null ? buildingMaterial : MakeColor(new Color(0.55f, 0.40f, 0.30f));
            }
            b.name = id;
            b.isStatic = true;

            // Door on the south side of the building.
            var door = GameObject.CreatePrimitive(PrimitiveType.Cube);
            door.name = "Door_" + id;
            door.tag = "Door";
            door.transform.SetParent(b.transform);
            door.transform.localPosition = new Vector3(0, -0.2f, -0.51f);
            door.transform.localScale = new Vector3(0.25f, 0.55f, 0.05f);
            door.GetComponent<Renderer>().material =
                doorMaterial != null ? doorMaterial : MakeColor(new Color(0.88f, 0.72f, 0.40f));
            door.AddComponent<Interactable>().Setup(InteractableKind.Door, id);
        }

        void BuildTrees()
        {
            for (int i = 0; i < 12; i++)
            {
                Vector3 p = new Vector3(Random.Range(-25f, 25f), 0, Random.Range(-25f, 25f));
                if (Mathf.Abs(p.x) < 5f || Mathf.Abs(p.z) < 5f) continue; // keep roads clear
                CreateTree(p);
            }
        }
        void CreateTree(Vector3 pos)
        {
            GameObject t;
            if (treePrefab != null) { t = Instantiate(treePrefab, pos, Quaternion.identity, _root); }
            else
            {
                t = new GameObject("Tree");
                t.transform.SetParent(_root);
                t.transform.position = pos;
                var trunk = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                trunk.transform.SetParent(t.transform);
                trunk.transform.localPosition = Vector3.up * 1.2f;
                trunk.transform.localScale = new Vector3(0.3f, 1.2f, 0.3f);
                trunk.GetComponent<Renderer>().material = MakeColor(new Color(0.34f, 0.20f, 0.10f));
                var canopy = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                canopy.transform.SetParent(t.transform);
                canopy.transform.localPosition = Vector3.up * 3.0f;
                canopy.transform.localScale = Vector3.one * 2.6f;
                canopy.GetComponent<Renderer>().material = MakeColor(new Color(0.20f, 0.50f, 0.20f));
            }
            t.tag = "Interactable";
            t.AddComponent<Interactable>().Setup(InteractableKind.Tree, "tree");
            t.isStatic = true;
        }

        void BuildVehicles()
        {
            CreateVehicle("Bus",  new Vector3(-12, 0.5f, -3f), new Vector3(8, 2, 3), new Color(0.85f, 0.75f, 0.30f));
            CreateVehicle("Car1", new Vector3(  5, 0.4f,  4f), new Vector3(3, 1, 2), new Color(0.80f, 0.30f, 0.30f));
        }
        void CreateVehicle(string id, Vector3 pos, Vector3 size, Color col)
        {
            GameObject v;
            if (vehiclePrefab != null) { v = Instantiate(vehiclePrefab, pos, Quaternion.identity, _root); }
            else
            {
                v = GameObject.CreatePrimitive(PrimitiveType.Cube);
                v.transform.SetParent(_root);
                v.transform.position = pos;
                v.transform.localScale = size;
                v.GetComponent<Renderer>().material = MakeColor(col);
            }
            v.name = id;
            v.tag = "Interactable";
            v.AddComponent<Interactable>().Setup(InteractableKind.Vehicle, id);
        }

        void BuildNPCs()
        {
            CreateNPC("classmate",  new Vector3( 14,  0,  -8), "زميلة دراسة");
            CreateNPC("professor",  new Vector3( 18,  0, -10), "أستاذ");
            CreateNPC("waiter",     new Vector3(-14,  0,  14), "النادل");
            CreateNPC("librarian",  new Vector3( 18,  0,  16), "أمين المكتبة");
            CreateNPC("pedestrian1",new Vector3( -3,  0,  -6), "رجل في الشارع");
            CreateNPC("pedestrian2",new Vector3(  4,  0, -10), "امرأة لطيفة");
            CreateNPC("oldfriend",  new Vector3(-15,  0,  18), "صديق قديم");
        }
        void CreateNPC(string id, Vector3 pos, string displayName)
        {
            GameObject npc;
            if (npcPrefab != null) { npc = Instantiate(npcPrefab, pos, Quaternion.identity, _root); }
            else
            {
                npc = new GameObject(id);
                npc.transform.SetParent(_root);
                npc.transform.position = pos;
                var body = GameObject.CreatePrimitive(PrimitiveType.Capsule);
                body.transform.SetParent(npc.transform);
                body.transform.localPosition = Vector3.up * 1f;
                body.transform.localScale = new Vector3(0.6f, 0.9f, 0.6f);
                body.GetComponent<Renderer>().material =
                    npcMaterial != null ? npcMaterial : MakeColor(RandomNpcColor(id));
                var head = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                head.transform.SetParent(npc.transform);
                head.transform.localPosition = Vector3.up * 2.0f;
                head.transform.localScale = Vector3.one * 0.5f;
                head.GetComponent<Renderer>().material = MakeColor(new Color(0.92f, 0.78f, 0.62f));
            }
            npc.name = id;
            npc.tag = "NPC";
            npc.AddComponent<Interactable>().Setup(InteractableKind.Person, id, displayName);
        }

        void BuildPlayer()
        {
            GameObject player;
            if (playerPrefab != null) { player = Instantiate(playerPrefab, new Vector3(0, 0, 0), Quaternion.identity); }
            else
            {
                player = new GameObject("Player");
                player.transform.position = new Vector3(0, 1f, 0);
                var body = GameObject.CreatePrimitive(PrimitiveType.Capsule);
                body.transform.SetParent(player.transform);
                body.transform.localScale = new Vector3(0.6f, 0.9f, 0.6f);
                body.GetComponent<Renderer>().material = MakeColor(new Color(0.18f, 0.22f, 0.32f));
                var head = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                head.transform.SetParent(player.transform);
                head.transform.localPosition = Vector3.up * 1.0f;
                head.transform.localScale = Vector3.one * 0.5f;
                head.GetComponent<Renderer>().material = MakeColor(new Color(0.95f, 0.82f, 0.65f));
                var cane = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                cane.name = "Cane";
                cane.transform.SetParent(player.transform);
                cane.transform.localPosition = new Vector3(0.4f, -0.3f, 0.6f);
                cane.transform.localRotation = Quaternion.Euler(70f, 0, 20f);
                cane.transform.localScale = new Vector3(0.04f, 0.6f, 0.04f);
                cane.GetComponent<Renderer>().material = MakeColor(Color.white);
            }
            player.tag = "Player";
            var rb = player.AddComponent<Rigidbody>();
            rb.constraints = RigidbodyConstraints.FreezeRotation;
            rb.collisionDetectionMode = CollisionDetectionMode.Continuous;
            var cap = player.AddComponent<CapsuleCollider>();
            cap.height = 1.8f;
            cap.radius = 0.35f;
            cap.center = new Vector3(0, 0.9f, 0);
            player.AddComponent<PlayerController>();
            player.AddComponent<InteractionFinder>();

            // Camera follow
            var camRig = new GameObject("CameraRig");
            camRig.transform.SetParent(player.transform);
            camRig.transform.localPosition = new Vector3(0, 4f, -6f);
            camRig.transform.localRotation = Quaternion.Euler(30f, 0, 0);
            if (Camera.main != null)
            {
                Camera.main.transform.SetParent(camRig.transform);
                Camera.main.transform.localPosition = Vector3.zero;
                Camera.main.transform.localRotation = Quaternion.identity;
                Camera.main.fieldOfView = 60f;
                Camera.main.gameObject.AddComponent<CameraFollow>().target = player.transform;
            }
        }

        void BuildSky()
        {
            RenderSettings.skybox = null;
            RenderSettings.ambientLight = new Color(0.55f, 0.58f, 0.65f);
            RenderSettings.fog = true;
            RenderSettings.fogColor = new Color(0.78f, 0.84f, 0.90f);
            RenderSettings.fogMode = FogMode.Linear;
            RenderSettings.fogStartDistance = 30f;
            RenderSettings.fogEndDistance = 80f;

            var sunGo = new GameObject("Sun");
            var sun = sunGo.AddComponent<Light>();
            sun.type = LightType.Directional;
            sun.color = new Color(1f, 0.95f, 0.85f);
            sun.intensity = 1.05f;
            sun.shadows = LightShadows.Soft;
            sun.transform.rotation = Quaternion.Euler(50f, -30f, 0);
        }

        void BuildNavMesh()
        {
            // Simple runtime NavMesh build using a single ground extent.
            var data = new NavMeshData();
            NavMesh.AddNavMeshData(data);
            // Note: building runtime navmesh from primitives requires
            // NavMeshBuilder.CollectSources + UpdateNavMeshData. The
            // simpler path is to bake from the editor before shipping.
            // The NavGuide script falls back to straight-line guidance
            // when no navmesh is present.
        }

        // --- helpers ---
        static int _colorSeed;
        static Color RandomNpcColor(string id)
        {
            int h = id == null ? 0 : id.GetHashCode();
            Random.InitState(h);
            return Color.HSVToRGB(Random.value, 0.55f, 0.85f);
        }
        static Material MakeColor(Color c)
        {
            var mat = new Material(Shader.Find("Standard"));
            mat.color = c;
            mat.SetFloat("_Glossiness", 0.2f);
            return mat;
        }
    }

    public class NavMeshSurfaceMarker : MonoBehaviour {}
}
