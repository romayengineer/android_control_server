#!/bin/bash

# Launch the app and tail logs for 10 seconds

echo "Launching app..."
adb shell am start -n com.romayengineer.controlserver/.MainActivity

echo "Clearing logcat buffer..."
adb logcat -c

echo "Tailing logs for 15 seconds (click Start button NOW)..."
echo ""

adb logcat -v threadtime | grep -E "WiFiMouseService|ServerSocket|controlserver|romay" &
LOGPID=$!

sleep 15
kill $LOGPID 2>/dev/null

echo ""
echo "Done tailing logs."
