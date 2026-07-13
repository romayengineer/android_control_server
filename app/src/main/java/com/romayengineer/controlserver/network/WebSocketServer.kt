package com.romayengineer.controlserver.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.romayengineer.controlserver.LogManager
import com.romayengineer.controlserver.input.InputController
import com.romayengineer.controlserver.input.MouseButton
import com.romayengineer.controlserver.input.ScrollDirection
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer as JavaWebSocketServer
import java.net.InetSocketAddress

class WebSocketServer(
    port: Int,
    private val inputController: InputController
) : JavaWebSocketServer(InetSocketAddress(port)) {

    companion object {
        private const val TAG = "WebSocketServer"
    }

    private val gson = Gson()

    init {
        setReuseAddr(true)
        LogManager.i("WebSocket server initialized on port $port")
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientIP = conn.remoteSocketAddress?.address?.hostAddress ?: "unknown"
        LogManager.i("WebSocket client connected: $clientIP")
        Log.d(TAG, "WebSocket connection opened from $clientIP")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val clientIP = conn.remoteSocketAddress?.address?.hostAddress ?: "unknown"
        LogManager.i("WebSocket client disconnected: $clientIP")
        Log.d(TAG, "WebSocket connection closed: code=$code, reason=$reason")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            LogManager.d("WebSocket received: $message")
            val command = gson.fromJson(message, JsonObject::class.java)

            val commandType = command.get("command")?.asString ?: return
            LogManager.d("Command type: $commandType")

            val result = when (commandType) {
                "mousemove" -> {
                    // Check if this is relative movement (dx, dy) or absolute (x, y)
                    if (command.has("dx") || command.has("dy")) {
                        val dx = command.get("dx")?.asInt ?: 0
                        val dy = command.get("dy")?.asInt ?: 0
                        val scaledDx = dx * 10
                        val scaledDy = dy * 10
                        LogManager.d("Executing mousemove relative: dx=$dx, dy=$dy (scaled to $scaledDx, $scaledDy)")
                        inputController.moveMouseRelative(scaledDx, scaledDy)
                    } else {
                        val x = command.get("x")?.asInt ?: 0
                        val y = command.get("y")?.asInt ?: 0
                        LogManager.d("Executing mousemove absolute: $x, $y")
                        inputController.moveMouse(x, y)
                    }
                }
                "click" -> {
                    val buttonStr = command.get("button")?.asString ?: "LEFT"
                    val button = MouseButton.valueOf(buttonStr)

                    if (command.has("x") && command.has("y")) {
                        val x = command.get("x").asInt
                        val y = command.get("y").asInt
                        LogManager.d("Executing click($x, $y, $buttonStr)")
                        inputController.clickMouse(x, y, button)
                    } else {
                        LogManager.d("Executing click at current position with button $buttonStr")
                        inputController.clickMouse(button)
                    }
                }
                "scroll" -> {
                    val x = command.get("x")?.asInt ?: 0
                    val y = command.get("y")?.asInt ?: 0
                    val directionStr = command.get("direction")?.asString ?: "DOWN"
                    val direction = ScrollDirection.valueOf(directionStr)
                    val distance = command.get("distance")?.asInt ?: 3
                    LogManager.d("Executing scroll($x, $y, $directionStr, $distance)")
                    inputController.scrollMouse(x, y, direction, distance)
                }
                "keypress" -> {
                    val keycode = command.get("keycode")?.asInt ?: return
                    LogManager.d("Executing keypress($keycode)")
                    inputController.pressKey(keycode)
                }
                "text" -> {
                    val text = command.get("text")?.asString ?: return
                    LogManager.d("Executing typeText($text)")
                    inputController.typeText(text)
                }
                else -> {
                    LogManager.w("Unknown command: $commandType")
                    false
                }
            }

            val response = JsonObject().apply {
                addProperty("success", result)
                addProperty("command", commandType)
            }

            conn.send(response.toString())
            LogManager.d("Command execution result: $result")
        } catch (e: Exception) {
            LogManager.e("Error processing WebSocket message: ${e.message}", e)
            Log.e(TAG, "Error processing message", e)

            val errorResponse = JsonObject().apply {
                addProperty("success", false)
                addProperty("error", e.message)
            }
            conn.send(errorResponse.toString())
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        LogManager.e("WebSocket error: ${ex.message}", ex)
        Log.e(TAG, "WebSocket error", ex)
    }

    override fun onStart() {
        LogManager.i("WebSocket server started successfully")
        Log.d(TAG, "WebSocket server started on port ${address.port}")
    }
}
