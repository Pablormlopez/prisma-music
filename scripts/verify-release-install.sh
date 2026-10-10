#!/usr/bin/env bash
set -euo pipefail
adb install Prisma-Music-0.1.1-beta.apk
adb logcat -c
adb shell am start -W -n com.prisma.music/.MainActivity
sleep 5
adb shell pidof com.prisma.music
adb logcat -d -s AndroidRuntime:E > release-crash-log.txt
if grep -q 'FATAL EXCEPTION' release-crash-log.txt; then
  cat release-crash-log.txt
  exit 1
fi
echo 'APK release instalada sin -t y abierta correctamente en Android 13.'
