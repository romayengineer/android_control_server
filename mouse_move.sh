#!/bin/bash

# WiFi Mouse Control Server - Mouse Movement Script
# Usage: ./mouse_move.sh <projector_ip> <x> <y>
# Example: ./mouse_move.sh 192.168.1.100 500 300

if [ $# -lt 3 ]; then
    echo "Usage: $0 <projector_ip> <x> <y>"
    echo "Example: $0 192.168.1.100 500 300"
    exit 1
fi

PROJECTOR_IP=$1
X=$2
Y=$3
PORT=3934

# Validate inputs
if ! [[ $X =~ ^[0-9]+$ ]] || ! [[ $Y =~ ^[0-9]+$ ]]; then
    echo "Error: X and Y coordinates must be numbers"
    exit 1
fi

# Send mousemove command
COMMAND="{\"command\": \"mousemove\", \"x\": $X, \"y\": $Y}"
RESPONSE=$(echo "$COMMAND" | nc -w 1 "$PROJECTOR_IP" "$PORT" 2>/dev/null)

if [ $? -eq 0 ] && [ ! -z "$RESPONSE" ]; then
    echo "✓ Moved mouse to ($X, $Y)"
    echo "Response: $RESPONSE"
else
    echo "✗ Failed to connect to $PROJECTOR_IP:$PORT"
    exit 1
fi
