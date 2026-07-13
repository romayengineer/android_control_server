# WiFi Mouse Control - Web Frontend

A modern, TypeScript-based Svelte web application for controlling the WiFi Mouse Server via WebSocket.

## Features

- **Draggable Circle Control**: Intuitive circular trackpad for mouse movement
- **Real-time Mouse Control**: Send mouse movements via WebSocket to the Android server
- **Click Support**: Left-click and right-click actions
- **Live Connection Status**: Visual indicator showing server connection state
- **Configuration**: Set custom server IP and port
- **Persistent Settings**: Remember server configuration between sessions
- **TypeScript**: Fully typed for type safety and better developer experience
- **Tailwind CSS**: Modern utility-first CSS framework for rapid UI development
- **Debounced Events**: Mousemove events debounced at 200ms to reduce network traffic
- **Console Logging**: Detailed event logging with emoji icons for easy debugging
- **Modern UI**: Clean, gradient-based dark theme with responsive design

## Requirements

- Node.js 16+
- npm or yarn
- WiFi connection to the Android control server

## Installation

```bash
cd frontend
npm install
```

## Development

Start the development server:

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

## Building

Create a production build:

```bash
npm run build
```

The built files will be in the `dist/` directory.

## Usage

1. **Start the Android Server**: Launch the WiFi Mouse Server app on your Android device
2. **Open the Web App**: Open this frontend in your browser
3. **Configure Connection**:
   - Enter the Android device's IP address (e.g., 192.168.1.100)
   - Enter the WebSocket port (default: 3935)
   - Click "Connect"
4. **Control the Mouse**:
   - **Drag within the circle**: Move the mouse pointer
   - **Click the circle**: Left-click action
   - **Right-click the circle**: Right-click action

## Features Explained

### Circle Control

The circular trackpad provides intuitive mouse control:
- Drag your finger/cursor within the circle to move the mouse
- The position relative to the circle center determines the cursor movement direction
- Visual feedback shows your current position within the circle

### Connection Management

- **Auto-save**: Server IP and port are saved to browser localStorage
- **Connection Status**: Green indicator shows successful connection, red shows disconnected
- **Manual Disconnect**: Click the "Disconnect" button to end the session and reconfigure

### Responsive Design

The app works on:
- Desktop browsers (Chrome, Firefox, Safari, Edge)
- Tablets
- Large phones (landscape orientation recommended)

### Event Debouncing

Mouse movement events are debounced with a 200ms delay to optimize network usage:
- **Mousemove events**: Debounced - only sent after 200ms of inactivity
- **Click events**: Sent immediately without debouncing
- **Benefits**: Reduces network traffic, improves performance on slow connections
- **Console log**: Shows as `📤 Event sent (debounced):` in browser console

### Console Logging

Comprehensive logging for debugging (press F12 to open Developer Tools):

**Connection Events:**
- ✅ `✅ WebSocket connected to ws://...` - Successful connection
- 🔌 `🔌 WebSocket disconnected` - Connection closed
- ❌ `❌ WebSocket error: ...` - Connection failed

**Command Events:**
- 📤 `📤 Event sent: {command: "mousemove", ...}` - Immediate events
- 📤 `📤 Event sent (debounced): {...}` - Debounced mousemove
- 🖱️ `🖱️ Left click` - Left-click action
- 🖱️ `🖱️ Right click` - Right-click action

**Server Responses:**
- 📥 `📥 Server response: {success: true, ...}` - Server acknowledgment

## Architecture

### Components

- **MouseControl.svelte**: Main component handling WebSocket connection, user interface, and mouse control logic
- **App.svelte**: Root component

### WebSocket Protocol

The app communicates with the Android server using JSON messages:

**Mouse Movement (Relative):**
```json
{"command": "mousemove", "dx": 50, "dy": 30}
```
The `dx` and `dy` values are relative movements (deltas) ranging from -100 to 100:
- **Range**: -100 (opposite direction) to 100 (full circle radius in that direction)
- **Benefits**: Works on any screen size, unbounded movement
- **Example**: Moving the circle pointer to the right-middle sends `dx: 100, dy: 0`

**Click:**
```json
{"command": "click", "button": "LEFT"}
```

**Right-click:**
```json
{"command": "click", "button": "RIGHT"}
```

### Server Responses

The server responds to each command with:
```json
{"success": true, "command": "mousemove"}
{"success": false, "error": "Command failed"}
```

## TypeScript Configuration

The project is fully typed with:
- Strict mode enabled
- DOM and browser APIs typed
- Svelte component types
- Type checking for all event handlers and WebSocket communication

## Build Configuration

- **Vite**: Lightning-fast build tool
- **Svelte**: Reactive component framework
- **TypeScript**: Type-safe JavaScript
- **Tailwind CSS**: Utility-first CSS framework with PostCSS and Autoprefixer
- **Production Build Size**: ~40KB JS (gzipped: ~16KB), ~12KB CSS (gzipped: ~3KB)

## Troubleshooting

### Can't connect to server
1. Verify the Android device IP address is correct
2. Ensure both devices are on the same WiFi network
3. Check that the Android server is running
4. Verify the port number (default 3935)
5. Check browser console for error messages

### Mouse movements are slow
1. Check your WiFi signal strength
2. Verify the server and client are on the same network
3. Check Android device performance (logs in the app UI)

### Settings not being saved
- Clear browser localStorage and reconfigure
- Check browser permissions for localStorage

### Debugging with Console Logs

1. **Open Developer Tools**: Press `F12` in your browser
2. **Go to Console tab**: Click the "Console" tab
3. **Watch for events**: You'll see emoji-prefixed log messages as you interact with the app
4. **Connection issues**: Look for ❌ errors or 🔌 disconnect messages
5. **Performance**: Count 📤 messages to see event frequency (should be debounced)

**Example console output when dragging:**
```
✅ WebSocket connected to ws://192.168.1.100:3935
📤 Event sent (debounced): {command: "mousemove", dx: 75, dy: 50}
📤 Event sent (debounced): {command: "mousemove", dx: 100, dy: 0}
🖱️ Left click
📤 Event sent: {command: "click", button: "LEFT"}
📥 Server response: {success: true, command: "click"}
```

Notice the `dx` and `dy` values are relative deltas, not absolute positions. This allows unbounded mouse control regardless of the target device's screen resolution.

## Development Notes

### TypeScript Features Used
- `lang="ts"` in Svelte script blocks
- Type annotations for variables, parameters, and return types
- Interface definitions for configuration
- Event typing with TypeScript event handler types
- WebSocket message type safety

### Svelte Lifecycle
- `onMount`: Initializes WebSocket connection and event listeners
- `onDestroy`: Cleans up debounce timers, event listeners, and WebSocket connection
- Reactive statements for connection status updates

### Tailwind CSS Integration
- **Config Files**: `tailwind.config.js` and `postcss.config.js`
- **CSS Entry**: `src/app.css` contains `@tailwind` directives
- **Utility Classes**: Extensively used in template for styling
- **Custom Colors**: Extended color palette with `status-green` and `status-red`
- **Production**: CSS automatically purged and minified in build output

### Event Debouncing Implementation
- **Function**: `sendCommandDebounced()` handles debounced mousemove events
- **Timer Management**: `debounceTimer` tracks active debounce timers
- **Last Command**: `lastCommand` stores the most recent command while debouncing
- **Cleanup**: Properly clears timers on component unmount
- **Delay**: Configurable via `DEBOUNCE_DELAY` constant (200ms)

## Future Enhancements

- **Scroll wheel support**: Implement scroll gestures on the circle
- **Keyboard input integration**: Send keyboard commands via the interface
- **Touch gesture support**: Optimize for mobile and tablet touch input
- **Audio feedback**: Sound effects on clicks and connection events
- **Customizable debounce**: Allow users to adjust the debounce delay (0-500ms)
- **Sensitivity settings**: Adjustable cursor speed multiplier
- **Preset server profiles**: Save multiple device configurations
- **Keyboard shortcuts**: Quick connect/disconnect with hotkeys
- **Dark/light theme toggle**: User preference for UI theme
- **Gesture trails**: Visualize cursor movement paths
- **Statistics dashboard**: Show event count, latency, and connection duration

## License

MIT License - Same as main Android project

## Support

For issues or questions:
- Check the main project README at `../README.md`
- Review server API documentation at `../SERVER_API.md`
- Check browser console for detailed error messages
