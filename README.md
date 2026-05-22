# حياة كفيف — Blind Life (Unity 3D Edition)

> لعبة قصصية ثلاثية الأبعاد مصممة بإمكانية وصول كاملة للمكفوفين وضعاف البصر، تعمل أيضًا للمبصرين. الإصدار الحالي مبني على **Unity 2022.3 LTS** بنظام رسم 3D حقيقي.

> **النسخة السابقة (Canvas 2D Java):** محفوظة في تاريخ git على commit `16d3c3f`. للرجوع لها:
> ```bash
> git checkout 16d3c3f
> ```

---

## ما الذي تحتاجه قبل أن يبنى الـ APK

الـ APK **لن يُبنى تلقائيًا** إلا بعد إضافة ترخيص Unity إلى GitHub. هذه الخطوات لمرة واحدة فقط:

### الخطوة 1: تثبيت Unity Hub على جهازك

نزّل من: https://unity.com/download

### الخطوة 2: تثبيت محرر Unity 2022.3.40f1

في Unity Hub → Installs → Install Editor → اختر **2022.3.40f1 (LTS)**.

### الخطوة 3: إنشاء حساب Unity والحصول على ترخيص شخصي مجاني

1. سجّل حسابًا على https://id.unity.com
2. في Unity Hub → الإعدادات (⚙️) → Licenses → Add → Get a free personal license
3. وافق على شروط Unity Personal (مجاني للأفراد).

### الخطوة 4: استخراج ملف الترخيص للاستخدام في GitHub Actions

#### الطريقة الأسرع: GitHub Action لطلب الترخيص

```yaml
# ضع هذا في .github/workflows/activation.yml واحذفه بعد الحصول على الملف
name: Acquire activation file
on: workflow_dispatch
jobs:
  activation:
    runs-on: ubuntu-latest
    steps:
      - uses: game-ci/unity-request-activation-file@v2
        id: getManualLicenseFile
        with:
          unityVersion: 2022.3.40f1
      - uses: actions/upload-artifact@v4
        with:
          name: ManualLicenseFile.alf
          path: ${{ steps.getManualLicenseFile.outputs.filePath }}
```

شغّل الـ workflow، نزّل ملف `.alf` الناتج، ارفعه على:
https://license.unity3d.com/manual

سيُنزّل لك ملف `.ulf`. هذا ملف الترخيص.

### الخطوة 5: أضف ملف الترخيص كـ GitHub Secret

1. افتح المستودع على GitHub.
2. Settings → Secrets and variables → Actions → New repository secret.
3. الاسم: `UNITY_LICENSE`
4. القيمة: انسخ كامل محتوى ملف `.ulf` والصقه.
5. (اختياري) أضف `UNITY_EMAIL` و `UNITY_PASSWORD` إذا أردت استخدام ترخيص محترف.

### الخطوة 6: ادفع تغييرًا (أي تغيير)

كل push على فرع `main` أو `claude/**` يبني APK تلقائيًا. نزّله من Actions → آخر تشغيل → Artifacts → `BlindLife-Unity-APK`.

---

## بنية المشروع

```
.
├── Assets/
│   ├── Scenes/MainScene.unity        ← المشهد الرئيسي (شبه فارغ — كل العالم يُبنى برمجيًا)
│   ├── Editor/BuildScript.cs         ← يستخدمه CI لبناء الـAPK
│   └── Scripts/
│       ├── GameBootstrap.cs          ← يبني العالم: أرض، طرق، مبانٍ، شخصيات، لاعب
│       ├── PlayerController.cs       ← الحركة + الإيماءات + لوحة المفاتيح للتجريب
│       ├── InteractionFinder.cs      ← تفاعل ذكي مع تفضيل الأبواب
│       ├── Interactable.cs           ← كل شيء قابل للتفاعل
│       ├── AccessibilityManager.cs   ← TTS + اهتزاز موحّد
│       ├── TtsManager.cs             ← يستدعي android.speech.tts.TextToSpeech
│       ├── HapticManager.cs          ← يستدعي android.os.Vibrator
│       ├── EnvDescriber.cs           ← وصف عربي لما حول اللاعب
│       ├── NavGuide.cs               ← إرشاد صوتي إلى وجهة
│       ├── DialogueSystem.cs         ← حوارات أساسية
│       ├── GameManager.cs            ← مهام، يوم/ليل، حفظ
│       └── CameraFollow.cs           ← كاميرا تتبع لاعب
├── ProjectSettings/                  ← إعدادات Unity (يفتحها Unity Hub تلقائيًا)
├── Packages/manifest.json            ← قائمة حزم Unity
└── .github/workflows/build-unity-apk.yml
```

## استبدال الرسومات بأصول احترافية

كل شيء حاليًا مرسوم بـ **primitives** (cubes, capsules, spheres) كنقطة بداية. لتحويل المشروع لأسلوب GTA:

1. اشتر حزمة أصول من Unity Asset Store. توصيات:
   - **Synty Studios** (Simple City, POLYGON City Pack) — أسلوب stylized
   - **Realistic City Pack** — أسلوب واقعي
   - **Mixamo Characters** — شخصيات مجانية بـ animations

2. استورد الحزمة في Unity Editor.

3. افتح `GameBootstrap.cs` في Inspector، اسحب prefabs الأصول إلى الحقول:
   - `Player Prefab`
   - `Building Prefab`
   - `NPC Prefab`
   - `Tree Prefab`
   - `Vehicle Prefab`

4. أعد التشغيل — العالم يُبنى الآن بالأصول الجديدة.

## التحكم الحالي

- **سحب للأعلى**: مشي للأمام
- **سحب للأسفل**: توقف
- **سحب يمين/يسار**: استدارة
- **نقرتان**: تفاعل
- **ضغط مطوّل**: وصف ما حولك
- **مفاتيح للتجريب على PC**: الأسهم + Space (تفاعل) + Tab (وصف)

## المشاكل المعروفة في هذه النسخة الأولية

- **NavMesh غير محسوب**: المرشد الصوتي يستخدم خط مستقيم حاليًا. لتفعيل تجنّب العوائق:
  1. افتح المشهد في Unity Editor
  2. Window → AI → Navigation
  3. Bake
- **لا توجد رسوم متحركة للشخصيات**: حركة فقط بدون أرجل تتحرك. يحتاج Mixamo animations.
- **لا توجد مخاطر فعّالة**: نظام الدراجة/السيارة من إصدار Canvas لم يُنقل بعد.
- **لا توجد مهام**: نظام المهام في `GameManager` فارغ. سيُنقل من DayScript الخاص بـ Canvas.

كل هذه نقاط للجلسات القادمة.

## استرجاع نسخة Canvas العاملة

```bash
git checkout 16d3c3f
# الـ APK سيبنى من هذه النقطة عبر الـ workflow القديم
```

أو عبر artifact مخزّن لو Actions كان مفعّلًا وقتها.

## الترخيص

استخدام شخصي وتعليمي.
