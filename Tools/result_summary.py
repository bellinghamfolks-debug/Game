import json
import sys
with open(sys.argv[1]) as f:
    summary = json.load(f)
print("# نتائج اختبار لعبة من سيربح المليون\n")
for key, label in (("result","النتيجة"),("totalTestCount","إجمالي الاختبارات"),("passedTests","الاختبارات الناجحة"),("failedTests","الاختبارات الفاشلة"),("skippedTests","الاختبارات المتخطاة")):
    if key in summary:
        print(f"- {label}: {summary[key]}")
for entry in summary.get("devicesAndConfigurations", []):
    device = entry.get("device", {})
    print(f"- جهاز المحاكاة: {device.get('deviceName', '')}، iOS {device.get('osVersion', '')}")
print("\nتفاصيل كل حالة اختبار محفوظة في Tests.xcresult.")
