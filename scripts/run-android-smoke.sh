#!/usr/bin/env bash
set +e
# The Google APIs emulator's launcher can ANR during background startup.
# Tests launch Prisma explicitly; disable only that launcher, only on an emulator.
if [ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = "1" ]; then
  adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true
fi
gradle connectedDebugAndroidTest --no-daemon
test_result=$?
mkdir -p screenshots
adb pull /sdcard/Download/PrismaTest screenshots/
adb logcat -d -s AndroidRuntime:E PrismaTest:D ExoPlayerImplInternal:E > screenshots/android-diagnostics.txt
cat screenshots/android-diagnostics.txt
exit "$test_result"
