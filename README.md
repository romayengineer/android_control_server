# Android WiFi Control Server

Control your Android projector's mouse pointer, keyboard, and other input from another Android device over WiFi. Perfect for presentations, media playback, and interactive applications.

## Features

- **WiFi-based Control**: No Bluetooth pairing required, works over standard WiFi networks
- **Full Input Support**: Mouse movement, clicks, keyboard input, and text typing
- **Multi-button Support**: Left, right, and middle mouse buttons
- **Keyboard Events**: Support for all Android KeyEvent codes
- **Auto-start Server**: Server starts automatically when app opens - no setup needed
- **Smart Restart**: Detects and cleanly restarts existing server instances
- **Auto-start on Boot**: Server automatically starts when device boots
- **Persistent Service**: Uses foreground service with automatic restart on crash
- **Concurrent Clients**: Multiple client connections supported simultaneously
- **Extensible Architecture**: Interface-based design allows multiple input injection backends
- **Debug Logging**: Detailed logs for troubleshooting command execution

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
- **Kotlin** 2.0.10+ (for compatibility with Android Gradle Plugin 8.5+)
- **Gradle** 8.7+
- **Java** 17+ (required by Android Gradle Plugin 8.5+)

## Installation

### Building on macOS

#### Prerequisites

1. **Install Android Studio**
   ```bash
   # Option 1: Using Homebrew
   brew install android-studio
   
   # Option 2: Download directly from
   # https://developer.android.com/studio
   ```

2. **Set up Android SDK**
   - Launch Android Studio
   - Go to Android Studio → Preferences → Appearance & Behavior → System Settings → Android SDK
   - Install SDK Platform for API 24+ and Android SDK Build-Tools

3. **Set environment variables** (add to `~/.zshrc` or `~/.bash_profile`)
   ```bash
   export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_SDK_ROOT/tools:$ANDROID_SDK_ROOT/platform-tools
   ```
   Then reload: `source ~/.zshrc`

4. **Install Java 17+**
   ```bash
   brew install openjdk@17
   export JAVA_HOME=/usr/local/opt/openjdk@17
   ```

5. **Verify setup**
   ```bash
   adb version
   java -version  # Should be Java 17 or higher
   ```

#### Building the Server APK

```bash
# Clone the repository
git clone <repository-url>
cd android_control_server

# Make gradle executable
chmod +x ./gradlew

# Build the APK
./gradlew assembleDebug

# APK will be in: app/build/outputs/apk/debug/app-debug.apk
```

### Installing on Android Device

#### Prerequisites
- Connect Android device via USB
- Enable USB Debugging on the device
- Verify connection: `adb devices`

#### Installation Options

```bash
# Option 1: Using Android Studio
# - Open the project in Android Studio
# - Click "Run" or press Shift+F10

# Option 2: Using command line
./gradlew installDebug

# Option 3: Manual installation
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

1. **Automatic**:
   - Server starts automatically when you open the WiFi Mouse Server app
   - No need to click any button - it's ready to use immediately
   - Note the projector's IP address displayed on screen

2. **Auto-start on Boot**:
   - Server automatically starts when device boots
   - Runs as foreground service with persistent notification

3. **Smart Restart**:
   - If server is already running on the configured port, it will restart cleanly
   - Tap "Start Server" button to manually restart with a different port

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

### Client Scripts

A collection of shell scripts are provided to control the server from macOS/Linux:

#### mouse_event.sh - Unified Mouse Event Control

```bash
./mouse_event.sh <event_type> <x> <y> [button]
```

**Event Types:**
- `move` - Move mouse to coordinates
- `click` - Click at coordinates with optional button

**Examples:**

```bash
# Move mouse to position
./mouse_event.sh move 500 300

# Left click (default)
./mouse_event.sh click 500 300

# Right click
./mouse_event.sh click 500 300 RIGHT

# Middle click
./mouse_event.sh click 500 300 MIDDLE
```

## API Documentation

See [SERVER_API.md](SERVER_API.md) for complete command reference and JSON message formats.

### Quick Example (JSON)

```json
{"command": "mousemove", "x": 500, "y": 300}
{"command": "click", "button": "LEFT"}
{"command": "text", "text": "Hello World"}
{"command": "keypress", "keycode": 4}
```

## Project Structure

```
android_control_server/
├── app/
│   ├── src/main/
│   │   ├── java/com/romayengineer/controlserver/
│   │   │   ├── input/
│   │   │   │   ├── InputController.kt          # Interface for input injection
│   │   │   │   └── RootInputController.kt      # Root-based implementation
│   │   │   ├── service/
│   │   │   │   └── WiFiMouseService.kt         # Main service
│   │   │   ├── network/
│   │   │   │   └── ServerSocket.kt             # TCP server & command parser
│   │   │   ├── receiver/
│   │   │   │   └── BootReceiver.kt             # Auto-start on boot
│   │   │   └── MainActivity.kt                 # UI for server control
│   │   ├── res/
│   │   │   ├── drawable/                       # Launcher icon assets
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── mipmap-*/                       # App icons for different densities
│   │   │   └── values/
│   │   │       ├── colors.xml
│   │   │       ├── strings.xml
│   │   │       └── styles.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── local.properties
├── gradle.properties
├── build.gradle
├── settings.gradle
└── README.md
```

## Implementation Details

### InputController Interface
Abstract interface defining all input operations:
- `moveMouse(x: Int, y: Int): Boolean`
- `clickMouse(button: MouseButton): Boolean`
- `clickMouse(x: Int, y: Int, button: MouseButton): Boolean` - Click at specific coordinates
- `pressMouse(button: MouseButton): Boolean`
- `releaseMouse(button: MouseButton): Boolean`
- `scrollMouse(x: Int, y: Int, direction: ScrollDirection, distance: Int): Boolean`
- `pressKey(keyCode: Int): Boolean`
- `releaseKey(keyCode: Int): Boolean`
- `typeText(text: String): Boolean`
- And more...

### RootInputController
Uses Android's `input` shell command to inject events system-wide:
```kotlin
input mousemove 500 300
input tap 500 300
input keyevent 4
input text "Hello"
```

**Note**: The `tap` command directly injects touch events without requiring button codes.

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
<uses-permission android:name="android.permission.INJECT_EVENTS" />
```

**Note**: The `INJECT_EVENTS` permission requires the device to have appropriate system configuration. On emulators and devices without system-level access, use the `input` shell command approach (which is already implemented).

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
- Check logcat for startup errors: `adb logcat | grep WiFiMouseService`

### Commands not executing
- Confirm `input` command is available: `adb shell input --help`
- Check device has necessary permissions
- Verify client is sending valid JSON format
- View detailed logs: `adb logcat | grep -E "ServerSocket|RootInputController"`

### Connection refused
- Ensure server is running (check foreground service notification)
- Verify firewall isn't blocking port 3934
- Confirm projector and client are on same WiFi network
- Use adb port forwarding for testing: `adb forward tcp:3934 tcp:3934`

### Debugging with Logcat

Enable detailed debugging by running:
```bash
adb logcat -v threadtime | grep -E "ServerSocket|RootInputController|WiFiMouseService"
```

This will show:
- Commands received from client
- Command execution results
- Shell command output and errors
- Connection lifecycle events

## Contributing

Contributions are welcome! Areas for contribution:
- Accessibility Service implementation
- Client app development
- Additional input backends
- Testing and bug fixes

## License

MIT License

Copyright (c) 2026 Maximiliano Romay Figueroa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Author

Romay Engineer - https://github.com/romayengineer

## Support

For issues, feature requests, or questions:
- Open an issue on GitHub
- Check existing documentation in SERVER_API.md
