# دليل تفعيل الرسومات بحزم Kenney (مجاني تمامًا)

> هذا الدليل لمن لا يعرف Unity ويريد رسومات لطيفة بدون دفع أي مبلغ. النتيجة بأسلوب low-poly stylized — جودة جيدة جدًا لموبايل، مع ميزة كبيرة: **كل الأصول مرخّصة CC0** (يحق لك استخدامها تجاريًا بدون نسب).

---

## ⏱️ الوقت المتوقع

- **أول مرة (تثبيت Unity):** 45-60 دقيقة (مرة واحدة فقط)
- **تنزيل واستيراد حزم Kenney:** 20 دقيقة
- **زر الإعداد التلقائي:** 30 ثانية
- **بناء APK:** 5-10 دقائق

**المجموع:** ساعة ونصف من الصفر إلى APK جاهز.

---

## الخطوة 1 — تثبيت Unity

نفس خطوات Synty:

1. حمّل Unity Hub من https://unity.com/download
2. سجّل حساب Unity مجاني
3. ثبّت محرر **Unity 2022.3.40f1 LTS** مع:
   - ✅ Android Build Support
   - ✅ Android SDK & NDK Tools
   - ✅ OpenJDK
4. افتح المشروع من Unity Hub.

---

## الخطوة 2 — تنزيل حزم Kenney

داخل Unity Editor المفتوح، من القائمة العلوية:

اضغط **BlindLife → Kenney → Open downloads page**

ستفتح صفحة `kenney.nl/assets?q=3d` في المتصفح.

أو فتحها صفحة-صفحة:

| الحزمة | عنصر القائمة | المحتوى |
|---|---|---|
| **City Kit (Commercial)** | `BlindLife → Kenney → 1. Open: City Kit (Commercial)` | مبانٍ تجارية ومحلات ومكاتب |
| **City Kit (Suburban)** | `→ 2. Open: City Kit (Suburban)` | بيوت سكنية |
| **City Kit (Roads)** | `→ 3. Open: City Kit (Roads)` | طرق ورصيف ومشاة |
| **Car Kit** | `→ 4. Open: Car Kit` | سيارات وحافلات |
| **Nature Kit** | `→ 5. Open: Nature Kit` | أشجار ونباتات |
| **Mini Characters 1** | `→ 6. Open: Mini Characters 1` | شخصيات + لاعب |

في كل صفحة:
1. اضغط الزر الأخضر **Download Now**
2. اختر **Donate $0** (مجاني)
3. اضغط **Continue to Download**
4. سيُنزَّل ملف ZIP في مجلد Downloads.

**ينصح بتنزيل على الأقل:**
- City Kit Commercial (الأهم — المباني)
- Car Kit
- Nature Kit
- Mini Characters 1

---

## الخطوة 3 — استيراد الـ ZIPs إلى Unity

لكل ملف ZIP حمّلته:

1. في Unity: **BlindLife → Kenney → Import a downloaded ZIP into Assets/Kenney…**
2. ستفتح نافذة لاختيار ملف
3. اختر ملف الـ ZIP الذي حمّلته
4. اضغط Open

السكريبت سيقوم تلقائيًا بـ:
- فك ضغط الملف
- وضعه في `Assets/Kenney/<اسم الحزمة>/`
- إعادة فحص الأصول (يحوّل FBX إلى موديلات قابلة للاستخدام)

كرّر لكل ملف ZIP.

---

## الخطوة 4 — اضغط الزر السحري

اضغط: **BlindLife → Setup Graphics (One-click)**

السكريبت:
1. ✅ يثبت Universal Render Pipeline
2. ✅ ينشئ ملف URP Asset
3. ✅ يضيف Global Volume للـ post-processing
4. ✅ يضبط جودة الموبايل
5. ✅ يبحث عن ملفات Kenney بأنماط مثل:
   - `building_*` للمباني
   - `car_*` و `bus_*` للسيارات
   - `tree_*` للأشجار
   - `character_*` للشخصيات
6. ✅ يربطها تلقائيًا بـ GameBootstrap

تظهر نافذة:
```
✓ X steps applied.
✓ N prefab(s) wired into GameBootstrap.
```

---

## الخطوة 5 — اختبر اللعبة

اضغط زر **Play (▶)** في أعلى Unity Editor.

ستبدأ اللعبة في وضع المعاينة. ستظهر:
- مدينة كاملة بمباني Kenney
- شخصية لاعب من Kenney
- شخصيات NPC تتحرك في المدينة
- سيارات Kenney
- أشجار في الأماكن المتوقعة

استخدم الأسهم على لوحة المفاتيح للحركة (مع `Space` للتفاعل، `Tab` للوصف، `H` للتلميح).

اضغط Play مرة ثانية لإيقاف المعاينة.

---

## الخطوة 6 — بناء الـ APK

### الطريقة أ — مباشرة من Unity

1. **File → Build Settings**
2. اختر **Android** (لو لم تكن مختارة)
3. اضغط **Switch Platform** (أول مرة فقط، 5 دقائق)
4. اضغط **Build**
5. اختر اسم وموقع للـ APK
6. انتظر 5-15 دقيقة

### الطريقة ب — عبر GitHub Actions (موصى به)

إذا أعددت ترخيص Unity كـ secret في GitHub:

```bash
git add .
git commit -m "Added Kenney assets and wired graphics"
git push
```

اذهب إلى Actions → آخر تشغيل → Artifacts → نزّل `BlindLife-Unity-APK`.

---

## ⚠️ ملاحظة مهمة عن أصول Kenney

ملفات Kenney هي **FBX models**، ليست **Prefabs**. الفرق:
- FBX = نموذج 3D خام
- Prefab = نموذج + معلومات (collider، scripts، materials)

عند سحب FBX إلى المشهد، Unity ينشئ instance منه. AutoSetup يعالج هذا تلقائيًا.

لكن قد تحتاج:
- **إضافة Collider يدويًا** للمباني (Mesh Collider)
- **تعديل الحجم** (Kenney models أصغر من المعتاد)
- **تثبيت Materials** (قد تحتاج فتح FBX → Materials → Extract Materials)

---

## 🎁 خطوات اختيارية لتحسين الجودة

### 1. Mixamo Animations (مجاني)

شخصيات Kenney ثابتة. لإضافة حركة مشي:
1. ادخل https://www.mixamo.com (مع حساب Adobe مجاني)
2. ارفع نموذج شخصية Kenney
3. اختر animation "Walking"
4. حمّل **FBX for Unity**

### 2. Quixel Megascans (مجاني مع Unity)

تكستشرات واقعية لتطبيقها على الأرض:
1. https://quixel.com/megascans
2. سجّل بحساب Unity
3. ابحث "asphalt" أو "concrete"
4. اضغط Add to Library → Export to Unity

### 3. Better Lighting

داخل المحرر:
- Window → Rendering → Lighting
- Scene tab → Generate Lighting

---

## 🆘 إن لم يعمل شيء

### "0 prefab(s) wired"

السكريبت ما لقى Kenney files:
1. تأكد أنك ضغطت **Import ZIP** بنجاح
2. افتح Project view وتحقق من وجود مجلد `Assets/Kenney/`
3. أعد تشغيل **BlindLife → Setup Graphics**

### مبانٍ معكوسة

بعض ملفات Kenney تستخدم اتجاه Y سفلي:
- اختر مبنى في Hierarchy
- في Inspector: غيّر Rotation X من 0 إلى -90

### الشخصية لا تظهر

نموذج شخصية Kenney قد يحتاج Animator. حلّ مؤقت:
- استبدل Player Prefab بـ NPC Prefab (نفس النموذج)

---

## ✅ ملخص ما تحصل عليه

- **مدينة كاملة** بمبانٍ متنوعة
- **سيارات** مختلفة
- **شخصيات NPC** تتجوّل
- **شخصية لاعب** بكاميرا تتبع
- **أشجار** متعددة
- **إضاءة دورية** على مدار اليوم
- **post-processing** سينمائي

**كل ذلك بـ $0** ✨

---

## مقارنة Kenney vs Synty

| | Kenney (مجاني) | Synty POLYGON City ($79) |
|---|---|---|
| السعر | $0 | $79 |
| جودة الموديلات | متوسطة-جيدة | عالية |
| Prefabs جاهزة | لا (FBX) | نعم |
| التكامل مع AutoSetup | ✅ كامل | ✅ كامل |

ابدأ بـ Kenney. لو أعجبتك التجربة، رقّ لـ Synty لاحقًا.
