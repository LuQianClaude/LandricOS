#!/bin/bash
# build_android.sh -- copy the web frontend into the Android assets directory
# and build a debug APK.
#
# Requirements (must already be installed on the host):
#   - Android SDK (set ANDROID_HOME or ANDROID_SDK_ROOT)
#   - JDK 17
#   - Gradle wrapper present at android/gradlew (generate it once with
#     `gradle wrapper` inside android/ if it is missing)
#
# The frontend source lives in /workspace/frontend/. This script copies its
# contents into android/app/src/main/assets/web/ so the WebView can load
# file:///android_asset/web/index.html when no server URL is configured.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANDROID_DIR="${PROJECT_ROOT}/android"
WEB_ASSETS_DIR="${ANDROID_DIR}/app/src/main/assets/web"
FRONTEND_DIR="${PROJECT_ROOT}/frontend"

cd "${PROJECT_ROOT}"

if [ ! -d "${FRONTEND_DIR}" ]; then
    echo "[build_android] ERROR: frontend directory not found at ${FRONTEND_DIR}"
    echo "[build_android] The frontend is owned by another agent; ensure it is present."
    exit 1
fi

echo "[build_android] Copying frontend -> ${WEB_ASSETS_DIR}"
mkdir -p "${WEB_ASSETS_DIR}"
# Remove old bundled files but keep the .gitkeep marker so the dir exists in git.
find "${WEB_ASSETS_DIR}" -mindepth 1 ! -name '.gitkeep' -delete
cp -R "${FRONTEND_DIR}/." "${WEB_ASSETS_DIR}/"

cd "${ANDROID_DIR}"

if [ ! -x "./gradlew" ]; then
    echo "[build_android] ERROR: gradle wrapper not found at ${ANDROID_DIR}/gradlew"
    echo "[build_android] Generate it once with: cd ${ANDROID_DIR} && gradle wrapper"
    exit 1
fi

# Make sure the SDK is discoverable.
if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
    echo "[build_android] WARNING: ANDROID_HOME / ANDROID_SDK_ROOT not set; gradle may fail to locate the SDK"
fi

echo "[build_android] Building debug APK"
./gradlew assembleDebug --no-daemon

APK="${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "${APK}" ]; then
    echo "[build_android] Done. APK:"
    echo "${APK}"
else
    echo "[build_android] ERROR: expected APK not found at ${APK}"
    exit 1
fi
