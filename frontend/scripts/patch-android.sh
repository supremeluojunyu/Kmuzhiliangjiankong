#!/usr/bin/env bash
set -euo pipefail

MANIFEST="android/app/src/main/AndroidManifest.xml"
RES_XML="android/app/src/main/res/xml"
NSC="$RES_XML/network_security_config.xml"

if [[ ! -f "$MANIFEST" ]]; then
  echo "AndroidManifest not found, skip patch"
  exit 0
fi

mkdir -p "$RES_XML"
cat > "$NSC" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
EOF

if ! grep -q 'usesCleartextTraffic' "$MANIFEST"; then
  sed -i 's/<application/<application android:usesCleartextTraffic="true"/' "$MANIFEST"
fi

if ! grep -q 'networkSecurityConfig' "$MANIFEST"; then
  sed -i 's/android:usesCleartextTraffic="true"/android:usesCleartextTraffic="true" android:networkSecurityConfig="@xml\/network_security_config"/' "$MANIFEST"
fi

echo "Applied Android network security config for HTTP API"

if ! grep -q 'REQUEST_INSTALL_PACKAGES' "$MANIFEST"; then
  sed -i '/<uses-permission android:name="android.permission.INTERNET" \/>/a\    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" \/>' "$MANIFEST"
  echo "Added REQUEST_INSTALL_PACKAGES permission"
fi

GRADLE="android/app/build.gradle"
if [[ -f "$GRADLE" ]] && [[ -n "${APP_VERSION_NAME:-}" ]]; then
  sed -i "s/versionName \".*\"/versionName \"${APP_VERSION_NAME}\"/" "$GRADLE"
  echo "Set versionName=${APP_VERSION_NAME}"
fi

if [[ -f "$GRADLE" ]] && [[ -n "${APP_VERSION_CODE:-}" ]]; then
  sed -i "s/versionCode [0-9]*/versionCode ${APP_VERSION_CODE}/" "$GRADLE"
  echo "Set versionCode=${APP_VERSION_CODE}"
fi
