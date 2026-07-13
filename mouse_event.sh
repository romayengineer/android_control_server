#!/bin/bash

# WiFi Mouse Control Server - Mouse Event Script
# Usage: ./mouse_event.sh <phone_ip> <event_type> <x> <y> [button]
# Examples:
#   ./mouse_event.sh 192.168.1.100 move 500 300
#   ./mouse_event.sh 192.168.1.100 click 500 300 LEFT
#   ./mouse_event.sh 192.168.1.100 click 500 300

if [ $# -lt 3 ]; then
    echo "Usage: $0 [phone_ip] <event_type> <x> <y> [button]"
    echo ""
    echo "Event types: move, click"
    echo "Buttons: LEFT (default), RIGHT, MIDDLE"
    echo "Phone IP: defaults to localhost"
    echo ""
    echo "Examples:"
    echo "  $0 localhost move 500 300"
    echo "  $0 192.168.1.100 move 500 300"
    echo "  $0 192.168.1.100 click 500 300 LEFT"
    exit 1
fi

PHONE_IP=${1:-localhost}
EVENT_TYPE=${2}
X=${3}
Y=${4}
BUTTON=${5:-LEFT}
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

# Send command to phone
if printf '%s\n' "$COMMAND" | nc -w 2 $PHONE_IP $PORT > /dev/null 2>&1; then
    case "$EVENT_TYPE" in
        move)
            echo "✓ Moved mouse to ($X, $Y)"
            ;;
        click)
            echo "✓ Clicked at ($X, $Y) with $BUTTON button"
            ;;
    esac
else
    echo "✗ Failed to connect to $PHONE_IP:$PORT"
    exit 1
fi
