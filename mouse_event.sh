#!/bin/bash

# WiFi Mouse Control Server - Mouse Event Script
# Usage: ./mouse_event.sh <event_type> <x> <y> [button]
# Examples:
#   ./mouse_event.sh move 500 300
#   ./mouse_event.sh click 500 300 LEFT
#   ./mouse_event.sh click 500 300

if [ $# -lt 3 ]; then
    echo "Usage: $0 <event_type> <x> <y> [button]"
    echo "Event types: move, click"
    echo "Buttons: LEFT (default), RIGHT, MIDDLE"
    echo ""
    echo "Examples:"
    echo "  $0 move 500 300"
    echo "  $0 click 500 300 LEFT"
    exit 1
fi

EVENT_TYPE=$1
X=$2
Y=$3
BUTTON=${4:-LEFT}
PORT=3934

# Validate coordinates
if ! [[ $X =~ ^[0-9]+$ ]] || ! [[ $Y =~ ^[0-9]+$ ]]; then
    echo "Error: X and Y coordinates must be numbers"
    exit 1
fi

# Build command based on event type
case "$EVENT_TYPE" in
    move)
        COMMAND="{\"command\": \"mousemove\", \"x\": $X, \"y\": $Y}"
        ;;
    click)
        COMMAND="{\"command\": \"click\", \"x\": $X, \"y\": $Y, \"button\": \"$BUTTON\"}"
        ;;
    *)
        echo "Error: Unknown event type '$EVENT_TYPE'"
        echo "Valid types: move, click"
        exit 1
        ;;
esac

# Setup adb port forwarding
adb forward tcp:$PORT tcp:$PORT > /dev/null 2>&1

# Send command
if printf '%s\n' "$COMMAND" | nc -w 2 localhost $PORT > /dev/null 2>&1; then
    case "$EVENT_TYPE" in
        move)
            echo "✓ Moved mouse to ($X, $Y)"
            ;;
        click)
            echo "✓ Clicked at ($X, $Y) with $BUTTON button"
            ;;
    esac
else
    echo "✗ Failed to connect to localhost:$PORT"
    exit 1
fi
