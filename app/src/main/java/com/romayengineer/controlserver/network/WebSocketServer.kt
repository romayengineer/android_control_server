package com.romayengineer.controlserver.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.romayengineer.controlserver.LogManager
import com.romayengineer.controlserver.input.InputController
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
    private val commandProcessor = CommandProcessor(inputController)

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
            val result = commandProcessor.processCommand(command)

            val response = JsonObject().apply {
                addProperty("success", result.success)
                addProperty("command", result.command)
                if (result.error != null) {
                    addProperty("error", result.error)
                }
            }

            conn.send(response.toString())
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
