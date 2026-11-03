#!/bin/bash
set -euo pipefail
if [ "$#" != 4 ]; then echo 'Usage: sign-device.sh unsigned.ipa profile.mobileprovision identity signed.ipa' >&2; exit 2; fi
input=$1; profile=$2; identity=$3; output=$4
test -f "$input"; test -f "$profile"; test ! -e "$output"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q "$input" -d "$work"
app="$work/Payload/Millionaire.app"
test -d "$app"
security cms -D -i "$profile" > "$work/profile.plist"
python3 - "$work/profile.plist" "$app/Info.plist" "$work/entitlements.plist" <<'PY'
import sys, plistlib, datetime, fnmatch
with open(sys.argv[1],'rb') as f: profile = plistlib.load(f)
with open(sys.argv[2],'rb') as f: bundle = plistlib.load(f)['CFBundleIdentifier']
ent = profile['Entitlements']
app_id = profile['ApplicationIdentifierPrefix'][0] + '.' + bundle
if not fnmatch.fnmatchcase(app_id, ent['application-identifier']):
    sys.exit('The profile does not cover this bundle identifier.')
if profile['ExpirationDate'] <= datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None):
    sys.exit('The profile has expired.')
if not profile.get('ProvisionedDevices'):
    sys.exit('Expected a development or Ad Hoc device profile.')
signed = {'application-identifier': app_id, 'com.apple.developer.team-identifier': ent['com.apple.developer.team-identifier'], 'get-task-allow': ent.get('get-task-allow', False)}
with open(sys.argv[3],'wb') as f: plistlib.dump(signed,f)
PY
cp "$profile" "$app/embedded.mobileprovision"
codesign --force --sign "$identity" --entitlements "$work/entitlements.plist" --generate-entitlement-der "$app"
codesign --verify --strict --deep --verbose=2 "$app"
ditto -c -k --norsrc --keepParent "$work/Payload" "$output"
