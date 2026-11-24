#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="/usr/local/google/home/minsoojun/.local/jdk-17"
export ANDROID_HOME="/usr/local/google/home/minsoojun/.local/android-sdk"
export PATH="${JAVA_HOME}/bin:/usr/local/google/home/minsoojun/.local/gradle-8.7/bin:${PATH}"

PROJECT_DIR="/usr/local/google/home/minsoojun/work/kakao_sticker_android_app"
DRIVE_FOLDER_ID="1J7yxIojPBhQw8KqBZVa_Ox_Tp71PU-nE"

cd "${PROJECT_DIR}"
gradle assembleRelease --no-daemon

VER_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "${PROJECT_DIR}/app/build.gradle.kts" || echo "3.0")

mkdir -p "${PROJECT_DIR}/dist"
cp -f "${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk" "${PROJECT_DIR}/dist/kakao-sticker-studio.apk"
cp -f "${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk" "${PROJECT_DIR}/dist/kakao-sticker-studio-v${VER_NAME}.apk"
"${ANDROID_HOME}/build-tools/34.0.0/apksigner" verify --verbose --print-certs "${PROJECT_DIR}/dist/kakao-sticker-studio-v${VER_NAME}.apk"
ls -lh "${PROJECT_DIR}/dist/kakao-sticker-studio-v${VER_NAME}.apk"
echo "NOTE: Always bump versionCode & versionName in app/build.gradle.kts and upload dist/kakao-sticker-studio-v\${VER_NAME}.apk to Google Drive folder sharewithminsoojun (${DRIVE_FOLDER_ID})."
