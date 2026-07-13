# Android WiFi Control Server

Control your Android projector's mouse pointer, keyboard, and other input from another Android device over WiFi. Perfect for presentations, media playback, and interactive applications.

## Features

- **WiFi-based Control**: No Bluetooth pairing required, works over standard WiFi networks
- **Full Input Support**: Mouse movement, clicks, keyboard input, and text typing
- **Multi-button Support**: Left, right, and middle mouse buttons
- **Keyboard Events**: Support for all Android KeyEvent codes
- **Auto-start Service**: Server automatically starts on device boot
- **Persistent Service**: Uses foreground service with automatic restart on crash
- **Concurrent Clients**: Multiple client connections supported simultaneously
- **Extensible Architecture**: Interface-based design allows multiple input injection backends

## Architecture

The project consists of two main components:

### Server (Android Projector)
- Listens on TCP port 3934 for incoming client connections
- Accepts JSON-formatted commands
- Injects input events into the Android system
- Runs as a persistent foreground service

### Client (Android Phone) - Coming Soon
- Sends touch trackpad events to the server
- Provides a virtual touchpad interface
- Communicates via TCP/JSON protocol

## Requirements

### Server
- **Android Device**: Android 7.0+ (API 24+)
- **Root Access or Developer Mode**: Required for root input injection
  - Option 1: Device with root/SuperUser access
  - Option 2: Future Accessibility Service implementation for non-root operation
- **Network**: WiFi connectivity with server and client on same network

### Build Environment
- Android Studio or command line with Android SDK
- Kotlin 1.5+
- Gradle 7.0+
- Java 11+

## Installation

### Building the Server APK

```bash
# Clone the repository
git clone <repository-url>
cd android_control_server

# Build the APK
./gradlew assembleDebug

# APK will be in: app/build/outputs/apk/debug/
```

### Installing on Android Device

```bash
# Using Android Studio
# - Open the project in Android Studio
# - Click "Run" or use Shift+F10

# Using adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Granting Permissions

After installation, grant the required permissions:

```bash
# If device has root access
adb shell
su
pm grant com.romayengineer.controlserver android.permission.INTERNET
```

## Usage

### Starting the Server

1. **Via App UI**:
   - Open the WiFi Mouse Server app
   - Optionally change the port (default: 3934)
   - Tap "Start Server"
   - Note the projector's IP address

2. **Auto-start on Boot**:
   - Server automatically starts when device boots
   - Runs as foreground service with persistent notification

### Finding Your Projector's IP

```bash
# Via Android device terminal
ifconfig wlan0

# Or check your router's connected devices list
```

### Connecting from Client

The client connects to the server via:
- **Host**: Projector IP address (e.g., 192.168.1.100)
- **Port**: 3934 (or custom port configured)

## API Documentation

See [SERVER_API.md](SERVER_API.md) for complete command reference and JSON message formats.

### Quick Example

```json
{"command": "mousemove", "x": 500, "y": 300}
{"command": "click", "button": "LEFT"}
{"command": "text", "text": "Hello World"}
{"command": "keypress", "keycode": 4}
```

## Project Structure

```
android_control_server/
├── src/main/
│   ├── java/com/romayengineer/controlserver/
│   │   ├── input/
│   │   │   ├── InputController.kt          # Interface for input injection
│   │   │   └── RootInputController.kt      # Root-based implementation
│   │   ├── service/
│   │   │   └── WiFiMouseService.kt         # Main service
│   │   ├── network/
│   │   │   └── ServerSocket.kt             # TCP server & command parser
│   │   ├── receiver/
│   │   │   └── BootReceiver.kt             # Auto-start on boot
│   │   └── MainActivity.kt                 # UI for server control
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   └── values/
│   │       ├── colors.xml
│   │       ├── strings.xml
│   │       └── styles.xml
│   └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── README.md
```

## Implementation Details

### InputController Interface
Abstract interface defining all input operations:
- `moveMouse(x: Int, y: Int): Boolean`
- `clickMouse(button: MouseButton): Boolean`
- `pressKey(keyCode: Int): Boolean`
- `typeText(text: String): Boolean`
- And more...

### RootInputController
Uses Android's `input` shell command to inject events system-wide:
```kotlin
input mousemove 500 300
input tap 1
input keyevent 4
input text "Hello"
```

### WiFiMouseService
- Implements `Service` interface
- Starts foreground service on creation
- Spawns ServerSocket thread to listen for connections
- Uses `START_STICKY` for automatic restart

### ServerSocket
- Accepts TCP connections on specified port
- Handles each client in separate thread
- Parses JSON commands
- Routes to InputController
- Returns JSON responses

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Future Enhancements

- **AccessibilityServiceInputController**: Non-root input injection via Accessibility Service
- **Client App**: Full Android client with trackpad UI
- **Authentication**: Optional password/token authentication for security
- **Device Discovery**: mDNS/Bonjour service discovery
- **Gesture Support**: Multi-touch gesture recognition
- **Configuration UI**: Web-based admin panel
- **Logging**: Detailed activity logging and debugging

## Troubleshooting

### Server won't start
- Ensure device has root access or developer mode enabled
- Check that port 3934 is not in use by another app
- Verify INTERNET permission is granted

### Commands not executing
- Confirm `input` command is available: `adb shell input --help`
- Check device has necessary permissions
- Verify client is sending valid JSON format

### Connection refused
- Ensure server is running (check foreground service notification)
- Verify firewall isn't blocking port 3934
- Confirm projector and client are on same WiFi network

## Contributing

Contributions are welcome! Areas for contribution:
- Accessibility Service implementation
- Client app development
- Additional input backends
- Testing and bug fixes

## License

[Add your license here]

## Author

Romay Engineer - https://github.com/romayengineer

## Support

For issues, feature requests, or questions:
- Open an issue on GitHub
- Check existing documentation in SERVER_API.md
