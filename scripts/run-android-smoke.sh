#!/usr/bin/env bash
set +e
gradle connectedDebugAndroidTest --no-daemon
test_result=$?
mkdir -p screenshots
adb pull /sdcard/Download/PrismaTest screenshots/
adb logcat -d -s AndroidRuntime:E PrismaTest:D ExoPlayerImplInternal:E > screenshots/android-diagnostics.txt
cat screenshots/android-diagnostics.txt
exit "$test_result"
