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
- **System-wide Cursor Overlay**: Real-time red crosshair cursor visible across ALL apps - not just in the control app
- **Live Cursor Tracking**: Cursor automatically moves to target position before executing clicks or scrolls (natural mouse behavior)
- **Persistent Cursor**: Overlay service keeps cursor visible even when app is backgrounded or other apps are active
- **Auto-restart on Crash**: Uses START_STICKY and KeepAliveJobService to automatically restart if killed
- **Live Log Display**: Real-time logs displayed in the app UI with timestamps
- **Comprehensive Logging**: Detailed logs for all events - service lifecycle, client connections, command execution, input controller selection, and errors
- **Dark Mode Theme**: Modern dark UI with excellent contrast - light text on dark backgrounds for comfortable viewing
- **Permission Validation**: Automatic startup check validates all required permissions and AccessibilityService status
- **Permission Requests**: App automatically requests missing permissions on first launch with system dialogs
- **Accessibility Service Guide**: Dialog prompts user to enable AccessibilityService with direct link to Settings
- **Status Badge**: Visual indicator (green/red circle) shows at a glance if app is ready to use - green = all OK, red = missing permissions or AccessibilityService
- **Automated Build & Install**: PowerShell scripts for Windows - build, auto-connect, install, and launch in one command
- **Device Auto-Connect**: Automatically connects to offline/unauthorized devices before installation
- **Smart Device Detection**: Handles device names with spaces and multiple connected devices
- **WebSocket Server**: Browser-based control via WebSocket protocol on port 3935 (TCP 3934 + WebSocket 3935 run simultaneously)
- **Dual Protocol Support**: Same JSON command format works over both TCP and WebSocket connections
- **Auto-Reconnect**: Web frontend automatically attempts to reconnect every 3 seconds if connection is lost after initial connection
- **Connection Resilience**: Smart reconnection only activates after successful initial connection, not on failed first attempts

## Architecture

The project consists of a flexible multi-protocol server supporting both traditional TCP and modern WebSocket connections:

### Server (Android Projector)
Runs two parallel network servers simultaneously:

**TCP Server** - Port 3934
- Traditional socket-based communication
- Accepts JSON-formatted commands
- Single connection per client

**WebSocket Server** - Port 3935
- Browser-based control support
- Supports multiple concurrent client connections
- Same JSON command format as TCP
- Perfect for web clients (Svelte, React, Vue, etc.) running in browser

Both servers:
- Inject input events into the Android system
- Run as part of persistent foreground service
- Use shared InputController for unified command handling
- Return JSON responses with success status

### Client Options - Multiple Protocols
- **TCP Client**: Native Android app or shell scripts sending JSON over TCP port 3934
- **WebSocket Client**: Browser-based app (HTML/Svelte/React/Vue) connecting via WebSocket to port 3935
- **Mixed**: Multiple clients can connect simultaneously over different protocols

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

**Option 1: Using PowerShell Build Script (Recommended for Windows)**

```powershell
# Build debug APK
.\scripts\build.ps1

# Build release APK
.\scripts\build.ps1 -BuildType release
```

The build script automatically:
- Sets up environment variables
- Validates Java and Android SDK
- Builds the APK
- Shows build duration and APK size

**Option 2: Using Gradle Directly**

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
- Connect Android device via USB or WiFi ADB
- Enable USB Debugging on the device (for USB) or enable ADB over network
- Verify connection: `adb devices`

#### Installation Options

**Option 1: Using PowerShell Install Script (Recommended for Windows)**

The easiest way - builds, installs, and launches in one command:

```powershell
# Build and install to connected device
.\scripts\install.ps1

# With release build
.\scripts\install.ps1 -BuildType release
```

The install script automatically:
- Builds the APK
- Detects connected device
- Auto-connects to offline devices
- Installs the app
- Launches the app on the device
- Shows IP address and next steps

**Option 2: Using Android Studio**
```bash
# Option 2a: Click "Run" or press Shift+F10
# Option 2b: Using terminal
./gradlew installDebug
```

**Option 3: Manual installation with adb**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n "com.romayengineer.controlserver/.MainActivity"
```

### Scripts Organization

All build, installation, and control scripts are organized in the `scripts/` folder for cleaner project structure:

**PowerShell Scripts (Windows):**
- `scripts/build.ps1` - Build the APK
- `scripts/install.ps1` - Build, install, and launch to device
- `scripts/run_and_tail.ps1` - Build, install, and monitor logs for 15 seconds

**Shell Scripts (macOS/Linux):**
- `scripts/mouse_event.sh` - Send mouse commands to server
- `scripts/debug_click.sh` - Test click events
- `scripts/run_and_tail.sh` - Build, install, and monitor logs

### Build and Install Scripts

Windows users have convenient PowerShell scripts for building and installation in the `scripts/` folder:

**scripts/build.ps1** - Standalone build script
```powershell
# Build debug APK
.\scripts\build.ps1

# Build release APK
.\scripts\build.ps1 -BuildType release

# Clean and rebuild
.\scripts\build.ps1 -Clean
```

**scripts/install.ps1** - Complete build and install workflow
```powershell
# One-command setup: Build → Connect → Install → Launch
.\scripts\install.ps1

# With release build
.\scripts\install.ps1 -BuildType release
```

Features:
- ✓ Auto-detects connected devices
- ✓ Auto-connects to offline/unauthorized devices
- ✓ Handles device names with spaces
- ✓ Auto-launches app after installation
- ✓ Shows build metrics and progress
- ✓ Helpful error messages and troubleshooting tips

**scripts/run_and_tail.ps1** - Build, install, and monitor logs
```powershell
# Build, install, and tail logs for 15 seconds
.\scripts\run_and_tail.ps1

# With release build
.\scripts\run_and_tail.ps1 -BuildType release
```

Features:
- ✓ Builds and installs the app (same as install.ps1)
- ✓ Clears logcat buffer for clean logs
- ✓ Monitors app logs for 15 seconds in real-time
- ✓ Filters for relevant app events (WiFiMouseService, ServerSocket, etc.)
- ✓ Shows final log snapshot after monitoring stops
- ✓ Perfect for testing and debugging

### Granting Permissions

The app automatically requests all required permissions on first launch with system permission dialogs. Simply grant the permissions when prompted.

**Permissions Requested:**
- `INTERNET` - Required for TCP socket communication
- `RECEIVE_BOOT_COMPLETED` - Required for auto-start on boot
- `FOREGROUND_SERVICE` - Required to run as foreground service
- `SYSTEM_ALERT_WINDOW` - Required for system-wide overlay cursor

If you need to manually grant permissions via ADB:
```bash
adb shell pm grant com.romayengineer.controlserver android.permission.INTERNET
adb shell pm grant com.romayengineer.controlserver android.permission.RECEIVE_BOOT_COMPLETED
adb shell pm grant com.romayengineer.controlserver android.permission.FOREGROUND_SERVICE
adb shell pm grant com.romayengineer.controlserver android.permission.SYSTEM_ALERT_WINDOW
```

### Enabling AccessibilityService (Non-Root Option)

For non-root click and scroll support, enable the AccessibilityService:

**In-App Dialog (Easiest):**
When you launch the app, if AccessibilityService is not enabled, a dialog will appear offering to open the Accessibility settings for you. Just tap "Open Settings" to go directly there.

**Manual on Device:**
1. Open Settings
2. Navigate to Accessibility (or Accessibility Settings)
3. Find "ControlServer" or "Android WiFi Control Server"
4. Enable the service

**Via ADB:**
```bash
adb shell settings put secure enabled_accessibility_services com.romayengineer.controlserver/.service.ControlServerAccessibilityService
```

Once enabled, the app will automatically use AccessibilityService for click and scroll commands. The status badge will turn green when the service is active. No restart needed.

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

4. **Automatic Recovery**:
   - Server automatically restarts if the app crashes
   - Service recovers automatically if force-stopped by user
   - KeepAliveJobService monitors and restarts services every 15 minutes
   - No manual intervention needed - server will resume working

### Cursor Display

The app features a system-wide real-time cursor display:
- **System-wide Overlay**: Red circular crosshair visible across all apps on the device
- **Live Tracking**: Cursor moves instantly when you send mouse movement commands
- **Natural Behavior**: Cursor automatically moves to target position BEFORE clicking or scrolling
- **Persistent**: Remains visible even when the control app is backgrounded
- **Non-intrusive**: Uses semi-transparent overlay that doesn't interfere with other apps

### Live Log Display

The app includes real-time logging directly in the UI for easy debugging and monitoring:
- **Real-time Updates**: Logs appear instantly as events occur
- **Timestamped**: Each log entry shows HH:mm:ss timestamp
- **Auto-scrolling**: Automatically scrolls to show the latest logs
- **Selectable Text**: Users can select and copy log messages
- **Terminal Style**: Black background with green text for easy reading
- **Monospace Font**: Technical logs displayed in fixed-width font

**Logged Events Include:**
- Service lifecycle (startup, shutdown, errors)
- Client connections and disconnections
- All commands received and executed (mousemove, click, scroll, keypress, text, etc.)
- Command execution results (success/failure)
- Input controller selection (AccessibilityService vs RootInputController)
- Error messages with context
- Warnings and debug information

### Dark Mode Theme

The app features a modern dark theme designed for comfortable viewing and reduced eye strain:
- **Dark Background**: `#1a1a1a` (very dark gray) for the main UI
- **Light Text**: All text in white/light gray for excellent contrast and readability
- **Accent Colors**:
  - **Blue** (`#3b82f6`) - IP address display and input field accents
  - **Green** (`#10b981`) - Start button and log text highlighting
  - **Red** (`#ef4444`) - Stop button for clear visual distinction
- **Card Elements**: Slightly lighter backgrounds (`#2a2a2a`) for input fields and sections
- **Monospace Logs**: Green-on-black terminal-style logs area for technical readability

The dark theme provides:
- Reduced eye strain during extended usage
- Professional, modern appearance
- Clear visual hierarchy with accent colors
- High contrast for accessibility

### Finding Your Projector's IP

```bash
# Via Android device terminal
ifconfig wlan0

# Or check your router's connected devices list
```

### Connecting from Client

**TCP Connection** (traditional socket):
- **Host**: Projector IP address (e.g., 192.168.1.100)
- **Port**: 3934 (or custom port configured)
- Use raw socket or netcat: `nc 192.168.1.100 3934`

**WebSocket Connection** (for browser clients):
- **URL**: `ws://192.168.1.100:3935` (or custom port + 1)
- **JavaScript Example**: `new WebSocket('ws://192.168.1.100:3935')`
- Perfect for Svelte, React, or vanilla HTML/JavaScript web clients
- Supports multiple concurrent connections
- **Auto-Reconnect**: Web frontend automatically reconnects every 3 seconds if connection drops
  - Only activates after a successful initial connection
  - Does not attempt reconnection if initial connection fails (user can change IP/port and try again)
  - User can stop reconnection attempts at any time by clicking the Disconnect button

### Client Scripts

A collection of shell scripts are provided in the `scripts/` folder to control the server from macOS/Linux:

#### scripts/mouse_event.sh - Unified Mouse Event Control

```bash
./scripts/mouse_event.sh <event_type> <x> <y> [button]
```

**Event Types:**
- `move` - Move mouse to coordinates
- `click` - Click at coordinates with optional button

**Examples:**

```bash
# Move mouse to position
./scripts/mouse_event.sh move 500 300

# Left click (default)
./scripts/mouse_event.sh click 500 300

# Right click
./scripts/mouse_event.sh click 500 300 RIGHT

# Middle click
./scripts/mouse_event.sh click 500 300 MIDDLE
```

## API Documentation

See [SERVER_API.md](SERVER_API.md) for complete command reference and JSON message formats.

### Supported Commands (TCP and WebSocket)

Both TCP and WebSocket servers accept JSON command format:

**Mouse Control:**
```json
{"command": "mousemove", "dx": 50, "dy": 30}
{"command": "click", "x": 500, "y": 300, "button": "LEFT"}
{"command": "scroll", "x": 500, "y": 300, "direction": "DOWN", "distance": 3}
```

**Note on Mouse Movement:**
- **Relative Movement** (`dx`, `dy`): Web frontend sends relative deltas (-100 to 100) for unbounded control
- **Absolute Movement** (`x`, `y`): Traditional TCP clients can still send absolute coordinates
- The server handles both formats transparently

**Keyboard Control:**
```json
{"command": "keypress", "keycode": 4}
{"command": "text", "text": "Hello World"}
```

### TCP Connection Example

Using shell script to send raw JSON over TCP:
```bash
./scripts/mouse_event.sh move 500 300
./scripts/mouse_event.sh click 500 300 LEFT
```

### Web Frontend Client

The included web-based frontend (built with Svelte) provides an intuitive touch-friendly interface with intelligent connection management:

**Features:**
- Visual circular control pad for mouse movement with drag gestures
- Left click and right click support
- Real-time connection status indicator (green when connected, red when disconnected)
- Smart auto-reconnect that maintains connection stability
- Dark theme UI optimized for comfortable use in various lighting conditions
- Responsive design works on desktop browsers and tablets

**Auto-Reconnect Behavior:**
- Once connected to a server, if the connection drops, the frontend automatically attempts to reconnect every 3 seconds
- Status displays "Reconnecting..." while attempting to restore the connection
- No need to manually reconnect or reload the page
- User can click Disconnect to stop reconnection attempts and change server IP/port

### WebSocket Connection Example

Using JavaScript in a browser client:
```javascript
const ws = new WebSocket('ws://192.168.1.100:3935');

ws.onopen = () => {
    // Send relative mouse move command (web frontend example)
    ws.send(JSON.stringify({
        command: 'mousemove',
        dx: 75,     // relative delta X (-100 to 100)
        dy: 50      // relative delta Y (-100 to 100)
    }));

    // Send click command
    ws.send(JSON.stringify({
        command: 'click',
        button: 'LEFT'
    }));

    // Type text
    ws.send(JSON.stringify({
        command: 'text',
        text: 'Hello from WebSocket!'
    }));
};

ws.onmessage = (event) => {
    const response = JSON.parse(event.data);
    console.log('Command result:', response.success, 'Command:', response.command);
};

ws.onerror = (error) => {
    console.error('WebSocket error:', error);
};
```

**Note:** The web frontend uses relative `dx`/`dy` values for flexible unbounded control. Traditional TCP clients can use absolute `x`/`y` coordinates. The server handles both formats.

**Response Format (both TCP and WebSocket):**
```json
{"success": true, "command": "mousemove"}
{"success": true, "command": "click"}
{"success": false, "error": "Command execution failed"}
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
│   │   │   │   ├── OverlayService.kt                    # System-wide cursor overlay
│   │   │   │   ├── ControlServerAccessibilityService.kt # AccessibilityService implementation
│   │   │   │   └── KeepAliveJobService.kt               # Periodic service health check & restart
│   │   │   ├── network/
│   │   │   │   ├── ServerSocket.kt                      # TCP server & command parser
│   │   │   │   └── WebSocketServer.kt                   # WebSocket server for browser clients
│   │   │   ├── receiver/
│   │   │   │   └── BootReceiver.kt                      # Auto-start on boot
│   │   │   ├── ui/
│   │   │   │   └── CursorView.kt                        # Custom cursor with crosshair design
│   │   │   ├── LogManager.kt                            # Central logging system with UI display
│   │   │   ├── PermissionChecker.kt                     # Permission validation & status badge
│   │   │   └── MainActivity.kt                          # UI for server control + logs + cursor
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── badge_circle.xml                    # Status badge (green/red circle)
│   │   │   │   └── ...                                 # Launcher icon assets
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── mipmap-*/                       # App icons for different densities
│   │   │   └── values/
│   │   │       ├── colors.xml                   # Dark mode color palette
│   │   │       ├── strings.xml
│   │   │       └── styles.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── local.properties
├── gradle.properties
├── build.gradle
├── settings.gradle
├── scripts/
│   ├── build.ps1                      # PowerShell build script for Windows
│   ├── install.ps1                    # PowerShell build + install + launch script
│   ├── run_and_tail.ps1               # PowerShell build + install + monitor logs script
│   ├── mouse_event.sh                 # Shell script for sending mouse commands
│   ├── debug_click.sh                 # Shell script for debug click testing
│   └── run_and_tail.sh                # Shell script for building, installing and monitoring logs
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
Main Android service orchestrating network servers and input handling:
- **Dual Servers**: Starts both TCP (port 3934) and WebSocket (port 3935) servers
- **Parallel Execution**: Each server runs on separate thread for concurrent handling
- **Shared Controller**: Both servers route commands to same LazyInputController
- **Foreground Service**: Displays persistent notification and prevents killing
- **Auto-restart**: Uses `START_STICKY` for automatic restart on crash
- **Clean Shutdown**: Properly stops both servers in onDestroy()
- **Thread Management**: Handles thread lifecycle, interruption, and cleanup
- **Fallback Controller**: Maintains RootInputController as fallback input method

### ServerSocket
- Accepts TCP connections on specified port
- Handles each client in separate thread
- Parses JSON commands
- Routes to InputController
- Returns JSON responses

### WebSocketServer
Modern WebSocket implementation for browser-based clients:
- **Port**: Runs on TCP port + 1 (default 3935)
- **Concurrent Clients**: Supports multiple simultaneous WebSocket connections
- **Same Commands**: Accepts identical JSON command format as TCP server
- **JSON Parsing**: Uses Gson for automatic JSON serialization/deserialization
- **Connection Lifecycle**: Logs client connections, disconnections, and errors
- **Error Handling**: Returns JSON error responses with context on command failure
- **Response Format**: Returns `{"success": true/false, "command": "type"}` after each command
- **Input Injection**: Routes all commands to same InputController as TCP server
- **Thread-safe**: Each WebSocket connection handled independently

**Advantages over TCP:**
- Native browser support (no custom client needed)
- Bidirectional communication
- Built-in connection management
- Works with modern web frameworks (React, Vue, Svelte)
- Perfect for real-time web UIs

### CursorView
Custom Android View for visual cursor display:
- **Design**: Red circular crosshair with semi-transparent fill
- **Real-time Updates**: Cursor position updates on every mouse move command
- **Thread-safe**: Updates posted to main thread via Handler
- **Layout Integration**: Overlay view on top of control UI without interference
- **No Performance Impact**: Lightweight drawing using Canvas API

### OverlayService
System-wide cursor overlay service:
- **WindowManager Integration**: Creates system-wide overlay window visible on all apps
- **Non-blocking**: Uses FLAG_NOT_TOUCHABLE and FLAG_NOT_FOCUSABLE for transparency
- **Persistent**: Survives app backgrounding, continues showing cursor position
- **Automatic Restart**: Uses START_STICKY_COMPATIBILITY for crash recovery
- **Single Instance**: Managed by WiFiMouseService, ensures only one overlay exists

### KeepAliveJobService
Background job service for service health monitoring:
- **Periodic Checks**: Runs every 15 minutes to verify services are running
- **Auto-restart**: Automatically restarts WiFiMouseService or OverlayService if crashed
- **Battery Efficient**: Uses JobScheduler for system-optimized periodic execution
- **Boot Persistence**: Automatically scheduled when device boots
- **Reliable Recovery**: Ensures server continues running even after multiple crashes

### LogManager
Centralized logging system with real-time UI display:
- **Singleton Pattern**: Single instance manages all app logs
- **Timestamped Entries**: Each log shows HH:mm:ss timestamp
- **Memory Efficient**: Keeps last 500 logs in memory with auto-cleanup
- **Thread-safe**: Synchronizes access to log list
- **UI Integration**: Automatically updates TextView on main thread
- **Auto-scroll**: Scrolls to latest logs for visibility
- **Multiple Levels**: Support for debug, info, warning, and error logs
- **Android Logging**: Also logs to Android's standard logcat for debugging

### Dark Mode Theme
Modern dark UI implementation with carefully chosen colors:
- **Color Palette**: Defined in `colors.xml` with semantic naming
- **Background Colors**: Primary dark (`#1a1a1a`), card (`#2a2a2a`), darker (`#0d0d0d`)
- **Text Colors**: Primary white (`#ffffff`), secondary light gray (`#b0b0b0`)
- **Accent Colors**: Blue, green, and red for clear visual hierarchy
- **Contrast**: Minimum 4.5:1 contrast ratio for accessibility compliance
- **UI Elements**: All buttons, inputs, and text styled for dark theme consistency

### PermissionChecker & Status Badge
Automatic startup validation with visual feedback and intelligent user guidance:

**Startup Checks:**
- **Permission Validation**: Checks required permissions (INTERNET, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, SYSTEM_ALERT_WINDOW)
- **AccessibilityService Check**: Verifies if AccessibilityService is enabled via Settings.Secure API
- **Startup Check**: Runs automatically on app launch and when app returns from Settings

**Intelligent Permission Dialogs:**
- **Sequential Flow**: Dialogs appear one at a time - Display Over Other Apps dialog first, then Accessibility dialog
- **Smart Detection**: Uses `Settings.canDrawOverlays()` to properly detect if SYSTEM_ALERT_WINDOW is already granted
- **Non-persistent Dialogs**: Dialogs only appear if permission/service is actually missing - won't repeat if already enabled
- **Auto re-check**: When user returns from Settings, app automatically re-checks and updates status without showing dialog again
- **Direct Navigation**: "Open Settings" buttons take user directly to the specific settings page needed

**Automatic Requests:**
- **Display Over Other Apps**: Dialog with direct link to Settings if overlay permission missing
- **Accessibility Service**: Dialog with direct link to Accessibility settings if service not enabled
- **Dynamic Badge Updates**: After granting permissions, status badge automatically updates to reflect new state

**Status Badge:**
- 24dp circular indicator next to app title
- **Green**: All permissions granted AND AccessibilityService enabled = App is ready
- **Red**: Missing permissions OR AccessibilityService disabled = User action needed

**Logging & Feedback:**
- Detailed permission status logged for each check with visual indicators (✓/✗)
- Permission request results logged in real-time
- Non-intrusive - checks on startup without blocking app functionality
- Dialog state tracking prevents duplicate dialogs from appearing

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.INJECT_EVENTS" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

**Permission Details:**
- `INTERNET`: Required for TCP socket communication
- `RECEIVE_BOOT_COMPLETED`: Required for auto-start on boot
- `FOREGROUND_SERVICE`: Required to run as foreground service
- `INJECT_EVENTS`: Optional - used by RootInputController for shell-based input injection
- `BIND_ACCESSIBILITY_SERVICE`: Required to declare AccessibilityService
- `SYSTEM_ALERT_WINDOW`: Required for system-wide overlay cursor (may need grant via ADB or device settings)

**Accessibility Service Enabling:**
No special manifest permission grant is needed. Users enable the service through device Settings → Accessibility.

**Overlay Permission on Android 11+:**
On some devices, the overlay permission requires manual grant:
```bash
adb shell pm grant com.romayengineer.controlserver android.permission.SYSTEM_ALERT_WINDOW
```

## Future Enhancements

- **Client App**: Full Android client with trackpad UI
- **Keyboard Support**: Full keyboard input via AccessibilityService
- **Cursor Customization**: Selectable cursor styles, colors, and sizes
- **Click Animation**: Visual feedback when clicks are executed
- **Gesture Trails**: Show movement paths during extended operations
- **Tap Hold Support**: Long-press gestures via AccessibilityService
- **Authentication**: Optional password/token authentication for security
- **Device Discovery**: mDNS/Bonjour service discovery
- **Multi-touch Gestures**: Two-finger tap, pinch, and rotate via AccessibilityService
- **Configuration UI**: Mobile app settings panel for cursor preferences and server configuration
- **Advanced Logging**: Detailed activity logging and audit trail
- **Network Compression**: Reduce bandwidth for slow connections

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

### App Crashed or Force-Stopped

Don't worry! The app includes automatic recovery:
- Service automatically restarts if the app crashes
- If you force-stop the app, just send a command and the server will auto-restart
- KeepAliveJobService runs every 15 minutes to ensure services stay running
- You can verify recovery by checking logcat for "Service started" messages

### Debugging with In-App Logs

The easiest way to debug is using the built-in log display:
1. Open the WiFi Mouse Server app
2. Look at the "Logs:" section at the bottom of the screen
3. All events are logged in real-time with timestamps
4. Logs include:
   - Service startup/shutdown events
   - Client connections and disconnections
   - Command execution details
   - Input controller selection (AccessibilityService vs Root)
   - Any errors with context

The logs display the last 500 events and automatically scroll to show the latest activity.

### Debugging with Logcat

For more detailed system-level debugging, enable logcat:
```bash
adb logcat -v threadtime | grep -E "ServerSocket|RootInputController|WiFiMouseService|AccessibilityInputController|LazyInputController|LogManager"
```

This will show:
- Commands received from client
- Input controller selection (Accessibility vs Root)
- Gesture dispatch results
- Shell command output and errors
- Connection lifecycle events
- All log messages sent to the app's UI

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
