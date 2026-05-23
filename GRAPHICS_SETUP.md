# دليل تفعيل الرسومات — خطوة بخطوة

> هذا الدليل مخصص لك تحديدًا: لا تعرف Unity، لكن تريد رسومات GTA-like بأقل جهد.
> **مع سكريبت `BlindLife → Setup Graphics`، عملك الفعلي 4 خطوات فقط.**

---

## المتطلبات (تثبيت لمرة واحدة)

### الخطوة 0.1 — حمّل Unity Hub

ادخل: https://unity.com/download

- اضغط **Download Unity Hub for Windows/Mac**
- ثبّته (مثل أي برنامج).
- افتحه.

### الخطوة 0.2 — حساب Unity مجاني

داخل Unity Hub:
- اضغط أيقونة الحساب أعلى-يمين
- **Sign in → Create account**
- استخدم إيميلك العادي
- وافق على شروط **Unity Personal** (مجاني للأفراد بدخل < $100k).

### الخطوة 0.3 — تثبيت محرّك Unity

في Unity Hub:
- بوّابة **Installs** (يسار)
- **Install Editor → Official Releases**
- اختر **2022.3.40f1 (LTS)** ← مهم تكون نفس النسخة
- ✅ **Android Build Support**
- ✅ **Android SDK & NDK Tools**
- ✅ **OpenJDK**
- اضغط Install (سيستغرق 15-30 دقيقة، حجم ~7 جيجا).

### الخطوة 0.4 — فتح المشروع

```bash
git clone https://github.com/<your-github>/<repo>.git
```

في Unity Hub:
- **Projects → Open → اختر مجلد Game**
- Unity سيفتحه ويبني cache (10-30 دقيقة أول مرة).
- لا تخف من التحذيرات الصفراء، فقط الأخطاء الحمراء مهمة.

---

## المسار السريع — أربع خطوات للرسومات

### الخطوة 1 — اشترِ حزمة الأصول

افتح المتصفح:
👉 https://assetstore.unity.com/packages/3d/environments/urban/polygon-city-pack-95214

**POLYGON City Pack** من Synty Studios.
- السعر: **$79**
- ادفع بالبطاقة (نفس حساب Unity)
- بعد الدفع، اضغط **Add to My Assets**.

### الخطوة 2 — استورد الحزمة في Unity

في Unity Editor (المفتوح):
1. **Window → Package Manager**
2. أعلى-يسار: غيّر الـ dropdown من "In Project" إلى **"My Assets"**
3. ابحث عن **POLYGON City**
4. اضغط **Download** (سيأخذ ~10 دقائق)
5. اضغط **Import**
6. ستظهر نافذة بالملفات. اضغط **All → Import**.
7. انتظر اكتمال الاستيراد (5-15 دقيقة).

### الخطوة 3 — اضغط الزر السحري

في القائمة العلوية لـ Unity (نفس الأشرطة مثل File / Edit / Window):

سيظهر زر جديد اسمه **BlindLife**.

اضغط: **BlindLife → Setup Graphics (One-click)**

سيقوم السكريبت تلقائيًا بـ:
1. ✅ تثبيت Universal Render Pipeline
2. ✅ ضبط ملفات Render Pipeline
3. ✅ إضافة Global Volume للـ post-processing
4. ✅ ضبط الجودة للموبايل
5. ✅ البحث عن prefabs في حزمة Synty
6. ✅ ربطها تلقائيًا بـ GameBootstrap

بعد ثانيتين، تظهر نافذة:
```
✓ X steps applied.
✓ N prefab(s) wired into GameBootstrap.
Press Play (▶) to test.
```

### الخطوة 4 — ابنِ الـ APK

طريقتان:

**أ. مباشرة من Unity:**
- **File → Build Settings**
- Platform: Android
- **Build → اختر مكان حفظ APK**
- انتظر 5-10 دقائق

**ب. عبر GitHub Actions (موصى به):**
- ادفع التغييرات إلى GitHub:
  ```bash
  git add .
  git commit -m "Imported Synty + setup graphics"
  git push
  ```
- بعد دقائق، نزّل APK من **Actions → Artifacts → BlindLife-Unity-APK**.

---

## إن لم يعمل شيء — الحلول الشائعة

### "URP not imported yet"
السكريبت ثبّت URP لكن لم يكتمل. الحل:
- انتظر 30 ثانية حتى Unity يعيد التحقق
- اضغط **BlindLife → 2. Configure URP asset** يدويًا
- ثم **3. Add Post-processing Volume**

### "No GameBootstrap in scene"
المشهد فاضي. الحل:
- افتح MainScene
- في Hierarchy: **+ → Create Empty**
- سمّه "Bootstrap"
- في Inspector: **Add Component → Game Bootstrap**
- ارجع وأعد ضغط "Setup Graphics".

### "0 prefab(s) wired"
السكريبت ما لقى prefabs بأسماء معروفة. الحل اليدوي:
- اختر GameObject "Bootstrap" في Hierarchy
- في Inspector ستجد الحقول الفارغة:
  - Player Prefab
  - Building Prefab
  - NPC Prefab
  - Tree Prefab
  - Vehicle Prefab
- في Project view، تنقّل لـ:
  `Assets/PolygonCity/Prefabs/`
- اسحب أي مبنى إلى **Building Prefab**
- اسحب شخصية إلى **NPC Prefab** و **Player Prefab**
- اسحب شجرة إلى **Tree Prefab**
- اسحب سيارة إلى **Vehicle Prefab**

### الـ APK لا يبني في GitHub Actions
- تأكد من إضافة `UNITY_LICENSE` كـ secret (راجع README.md)
- تأكد أن الحزمة المستوردة في الـ repo (قد تحتاج عمل repo خاص لأن Synty محمي بحقوق ملكية).

---

## بعد الإعداد — تخصيصات اختيارية

### إضافة Mixamo animations (مجانًا)

شخصيات Synty جامدة افتراضيًا. لإضافة حركة مشي:
1. اذهب: https://www.mixamo.com (مجاني، يحتاج حساب Adobe)
2. اختر شخصية → ارفع نموذج Synty أو استخدم شخصيات Mixamo
3. اختر animation "Walking"
4. تنزيل: **FBX for Unity**
5. في Unity: اسحب الملف إلى `Assets/`
6. اربط Animator بشخصية اللاعب.

(هذه خطوة متقدمة، يمكنني كتابة سكريبت لها لاحقًا.)

### post-processing أعلى

افتح **Global Volume** في Hierarchy:
- في Inspector → **Add Override**:
  - **Bloom** → Intensity 0.6
  - **Color Adjustments** → Contrast +15, Saturation +20
  - **Tonemapping** → Mode ACES
  - **Vignette** → Intensity 0.3
  - **Depth of Field** → Mode Bokeh

---

## ملخص الوقت المتوقع

| خطوة | الوقت | مرة واحدة؟ |
|---|---|---|
| تثبيت Unity Hub | 5 دقائق | نعم |
| تثبيت Unity 2022.3 LTS + Android | 30 دقيقة | نعم |
| فتح المشروع أول مرة | 20 دقيقة | نعم |
| شراء واستيراد Synty Pack | 20 دقيقة | نعم |
| **BlindLife → Setup Graphics** | 30 ثانية | كلما تستورد حزمة |
| Build APK | 5 دقائق | كل تحديث |

**المجموع: ساعة و20 دقيقة من أول مرة، بعدها كل APK جديد 5 دقائق.**

---

## هل أحتاج تنفيذ شيء لك؟

لا — كل ما يمكنني عمله من هنا موجود في `AutoSetup.cs`. أعلمني فقط:
- إذا واجهت رسالة خطأ معينة (انسخها)
- إذا اشتريت حزمة مختلفة (وسأضيف patterns الأصول الخاصة بها)
- إذا تريد ميزة معيّنة بعد التثبيت (animations, إضاءة مخبوزة، إلخ).
