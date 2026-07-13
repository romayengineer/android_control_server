#!/bin/bash

# Debug script to show logs while testing click command

echo "Starting app..."
adb shell am force-stop com.romayengineer.controlserver
adb logcat -c
adb shell am start -n com.romayengineer.controlserver/.MainActivity

echo "Waiting 3 seconds for app to start..."
sleep 3

echo ""
echo "Tailing logs (waiting for you to run mouse_event command)..."
echo "In another terminal, run: ./mouse_event.sh click 500 1000"
echo ""
echo "Press Ctrl+C to stop logging"
echo ""

adb logcat -v threadtime | grep -E "ServerSocket|RootInputController"
