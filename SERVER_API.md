# WiFi Mouse Server API

## Overview
The server listens on TCP port 3934 (configurable) and accepts JSON commands over a single-line format (one JSON object per line).

## Connection
- **Host**: Projector IP address
- **Port**: 3934 (default, configurable via intent extra)
- **Protocol**: TCP with JSON messages (one per line)

## Message Format
All messages are JSON objects sent as a single line, followed by a newline character.

## Available Commands

### Mouse Movement
```json
{"command": "mousemove", "x": 100, "y": 200}
```

### Mouse Click
```json
{"command": "click", "button": "LEFT"}
```
Button options: `LEFT`, `RIGHT`, `MIDDLE` (default: `LEFT`)

### Mouse Press (Down)
```json
{"command": "mousedown", "button": "LEFT"}
```

### Mouse Release (Up)
```json
{"command": "mouseup", "button": "LEFT"}
```

### Mouse Scroll
```json
{"command": "scroll", "x": 100, "y": 200, "direction": "DOWN", "distance": 5}
```
Direction options: `UP`, `DOWN`, `LEFT`, `RIGHT`
Distance: number of scroll steps

### Key Press (single keystroke)
```json
{"command": "keypress", "keycode": 4}
```
Android KeyEvent codes: https://developer.android.com/reference/android/view/KeyEvent

### Key Down
```json
{"command": "keydown", "keycode": 4}
```

### Key Up
```json
{"command": "keyup", "keycode": 4}
```

### Type Text
```json
{"command": "text", "text": "Hello"}
```

## Response Format
The server responds with a JSON object containing:
```json
{"success": true, "command": "mousemove"}
```
or
```json
{"success": false, "error": "error message"}
```

## Common Android KeyEvent Codes
- 4: KEYCODE_BACK
- 3: KEYCODE_HOME
- 187: KEYCODE_MENU
- 24: KEYCODE_VOLUME_UP
- 25: KEYCODE_VOLUME_DOWN
- 26: KEYCODE_POWER

## Architecture

### InputController (Interface)
Abstract interface for input injection with methods:
- `moveMouse(x, y)`
- `clickMouse(button)`
- `pressMouse(button)`
- `releaseMouse(button)`
- `scrollMouse(x, y, direction, distance)`
- `pressKey(keyCode)`
- `releaseKey(keyCode)`
- `typeText(text)`
- `isAvailable()`
- `shutdown()`

### RootInputController
Implementation using `input` shell command (requires root/adb access)

### Future: AccessibilityServiceInputController
Will be implemented for non-root operation using Android Accessibility Service

## Service Details

### WiFiMouseService
- Runs as a foreground service (always stays running)
- Auto-starts on device boot via BootReceiver
- Restart policy: START_STICKY (will restart if killed)
- Listens on configurable port (default: 3934)

### Client Handling
- Accepts multiple concurrent client connections
- Each client connection handled in a separate thread
- Reads one JSON command per line
- Sends response back to client

## Example Client Implementation (Pseudocode)
```kotlin
val socket = Socket("192.168.1.100", 3934)
val writer = PrintWriter(socket.outputStream, true)
val reader = BufferedReader(InputStreamReader(socket.inputStream))

// Send command
writer.println("""{"command": "mousemove", "x": 100, "y": 200}""")

// Read response
val response = reader.readLine()
```
