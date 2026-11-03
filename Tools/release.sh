#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p dist/logs
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
xcodebuild -project Millionaire.xcodeproj -scheme Millionaire -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath "$work/Derived" BUNDLE_ID="${BUNDLE_ID:-app.millionaire.arabic}" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build | tee dist/logs/device-build.log
app="$work/Derived/Build/Products/Release-iphoneos/Millionaire.app"
test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleSupportedPlatforms:0' "$app/Info.plist")" = iPhoneOS
xcrun lipo -verify_arch arm64 "$app/Millionaire"
xcrun vtool -show-build "$app/Millionaire" | tee dist/logs/macho.txt
grep -Eq 'platform IOS$' dist/logs/macho.txt
test ! -e "$app/embedded.mobileprovision"
test ! -d "$app/_CodeSignature"
if codesign -d "$app" 2>/dev/null; then echo 'Unexpected code signature'; exit 1; fi
mkdir "$work/Payload"
ditto "$app" "$work/Payload/Millionaire.app"
ditto -c -k --norsrc --keepParent "$work/Payload" dist/Millionaire-iOS-Unsigned.ipa
ditto -c -k --norsrc --keepParent "$app" dist/Millionaire.app.zip
unzip -tq dist/Millionaire-iOS-Unsigned.ipa
unzip -Z1 dist/Millionaire-iOS-Unsigned.ipa | grep -Fx 'Payload/Millionaire.app/Millionaire'
shasum -a 256 dist/Millionaire-iOS-Unsigned.ipa | tee dist/logs/SHA256.txt
git archive --format=zip --prefix=Millionaire/ -o dist/Millionaire-source.zip HEAD
