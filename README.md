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
- **AccessibilityService Support**: Non-root click and scroll via Android AccessibilityService API
- **Dual Input Methods**: AccessibilityService (preferred) with RootInputController fallback
- **Visual Cursor**: Real-time cursor display with crosshair design - visible on app screen
- **Cursor Tracking**: Cursor position updates automatically with mouse movement commands
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
- **Input Method** (one of):
  - **AccessibilityService** (Recommended): Enable in device Accessibility settings - non-root, works for click and scroll
  - **Root Access**: Device with SuperUser access - allows full input injection
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

### Enabling AccessibilityService (Non-Root Option)

For non-root click and scroll support, enable the AccessibilityService:

**Via ADB:**
```bash
adb shell settings put secure enabled_accessibility_services com.romayengineer.controlserver/.service.ControlServerAccessibilityService
```

**Or Manually on Device:**
1. Open Settings
2. Navigate to Accessibility (or Accessibility Settings)
3. Find "ControlServer" or "Android WiFi Control Server"
4. Enable the service

Once enabled, the app will automatically use AccessibilityService for click and scroll commands. No restart needed.

## Usage

### Starting the Server

1. **Automatic**:
   - Server starts automatically when you open the WiFi Mouse Server app
   - No need to click any button - it's ready to use immediately
   - Note the projector's IP address displayed on screen
   - A red cursor crosshair will appear on the display to show mouse position

2. **Auto-start on Boot**:
   - Server automatically starts when device boots
   - Runs as foreground service with persistent notification

3. **Smart Restart**:
   - If server is already running on the configured port, it will restart cleanly
   - Tap "Start Server" button to manually restart with a different port

### Cursor Display

The app features a real-time cursor display:
- **Visible Cursor**: Red circular crosshair appears on the app screen
- **Live Tracking**: Cursor moves instantly when you send mouse movement commands
- **Non-intrusive**: Overlays on top of the UI without blocking buttons or text
- **Always Visible**: Shows your current mouse position on the projector

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
│   │   │   │   ├── InputController.kt                    # Interface for input injection
│   │   │   │   ├── RootInputController.kt               # Root-based implementation
│   │   │   │   ├── AccessibilityInputController.kt      # AccessibilityService-based implementation
│   │   │   │   ├── MouseButton.kt                       # Enum for mouse buttons
│   │   │   │   └── ScrollDirection.kt                   # Enum for scroll directions
│   │   │   ├── service/
│   │   │   │   ├── WiFiMouseService.kt                  # Main service with LazyInputController
│   │   │   │   └── ControlServerAccessibilityService.kt # AccessibilityService implementation
│   │   │   ├── network/
│   │   │   │   └── ServerSocket.kt                      # TCP server & command parser
│   │   │   ├── receiver/
│   │   │   │   └── BootReceiver.kt                      # Auto-start on boot
│   │   │   ├── ui/
│   │   │   │   └── CursorView.kt                        # Custom cursor with crosshair design
│   │   │   └── MainActivity.kt                          # UI for server control + cursor management
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

### AccessibilityInputController
Uses Android's `AccessibilityService` API for non-root input injection:
- Implements gesture-based input using `GestureDescription` and `dispatchGesture()`
- Click gestures: Creates a swipe from point to itself (100ms duration)
- Scroll gestures: Creates directional swipes with 500ms duration
- Supported directions: UP, DOWN, LEFT, RIGHT
- Works on any Android device without requiring root access
- Automatically used when AccessibilityService is enabled

### LazyInputController
Wrapper controller that dynamically checks for AccessibilityService:
- Checks on-demand (on each command) rather than at startup
- Uses AccessibilityService if available
- Falls back to RootInputController if AccessibilityService is not enabled
- Allows seamless switching without app restart

### RootInputController
Uses Android's `input` shell command to inject events system-wide:
```kotlin
input mousemove 500 300
input swipe 500 300 500 300 100  // Click via swipe
input keyevent 4
input text "Hello"
```

**Note**: Requires INJECT_EVENTS permission and device-level access.

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

### CursorView
Custom Android View for visual cursor display:
- **Design**: Red circular crosshair with semi-transparent fill
- **Real-time Updates**: Cursor position updates on every mouse move command
- **Thread-safe**: Updates posted to main thread via Handler
- **Layout Integration**: Overlay view on top of control UI without interference
- **No Performance Impact**: Lightweight drawing using Canvas API

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.INJECT_EVENTS" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```

**Permission Details:**
- `INTERNET`: Required for TCP socket communication
- `RECEIVE_BOOT_COMPLETED`: Required for auto-start on boot
- `FOREGROUND_SERVICE`: Required to run as foreground service
- `INJECT_EVENTS`: Optional - used by RootInputController for shell-based input injection
- `BIND_ACCESSIBILITY_SERVICE`: Required to declare AccessibilityService

**Accessibility Service Enabling:**
No special manifest permission grant is needed. Users enable the service through device Settings → Accessibility.

## Future Enhancements

- **Client App**: Full Android client with trackpad UI
- **Keyboard Support**: Full keyboard input via AccessibilityService
- **Cursor Customization**: Selectable cursor styles, colors, and sizes
- **Click Animation**: Visual feedback when clicks are executed
- **Gesture Trails**: Show movement paths during extended operations
- **Authentication**: Optional password/token authentication for security
- **Device Discovery**: mDNS/Bonjour service discovery
- **Multi-touch Gestures**: Two-finger tap, pinch, and rotate via AccessibilityService
- **Configuration UI**: Web-based admin panel for settings and cursor preferences
- **Advanced Logging**: Detailed activity logging and audit trail

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
adb logcat -v threadtime | grep -E "ServerSocket|RootInputController|WiFiMouseService|AccessibilityInputController|LazyInputController"
```

This will show:
- Commands received from client
- Input controller selection (Accessibility vs Root)
- Gesture dispatch results
- Shell command output and errors
- Connection lifecycle events

**AccessibilityService Specific:**
```bash
adb logcat | grep "AccessibilityInputController"
```

Look for:
- `Click dispatch result: true` - Gesture successfully queued
- `Accessibility service connected` - Service is active
- `Using AccessibilityService for input` - Service is being used

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
