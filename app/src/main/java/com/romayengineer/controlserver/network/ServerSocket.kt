package com.romayengineer.controlserver.network

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romayengineer.controlserver.input.InputController
import com.romayengineer.controlserver.input.MouseButton
import com.romayengineer.controlserver.input.ScrollDirection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket as JavaServerSocket

class ServerSocket(
    private val port: Int,
    private val inputController: InputController
) {
    companion object {
        private const val TAG = "ServerSocket"
    }

    private var serverSocket: JavaServerSocket? = null
    private var isRunning = true

    fun start() {
        try {
            serverSocket = JavaServerSocket(port)
            Log.d(TAG, "Server socket created on port $port")

            while (isRunning) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        Log.d(TAG, "Client connected: ${clientSocket.inetAddress.hostAddress}")
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error accepting client", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server socket error", e)
        } finally {
            stop()
        }
    }

    private fun handleClient(clientSocket: java.net.Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
                val writer = PrintWriter(clientSocket.outputStream, true)

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val response = processCommand(it)
                        writer.println(response)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
            } finally {
                clientSocket.close()
                Log.d(TAG, "Client disconnected")
            }
        }.start()
    }

    private fun processCommand(jsonString: String): String {
        return try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val command = json.get("command").asString

            val success = when (command) {
                "mousemove" -> {
                    val x = json.get("x").asInt
                    val y = json.get("y").asInt
                    inputController.moveMouse(x, y)
                }
                "click" -> {
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    inputController.clickMouse(button)
                }
                "mousedown" -> {
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    inputController.pressMouse(button)
                }
                "mouseup" -> {
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    inputController.releaseMouse(button)
                }
                "scroll" -> {
                    val x = json.get("x").asInt
                    val y = json.get("y").asInt
                    val direction = ScrollDirection.valueOf(json.get("direction").asString)
                    val distance = json.get("distance")?.asInt ?: 5
                    inputController.scrollMouse(x, y, direction, distance)
                }
                "keypress" -> {
                    val keyCode = json.get("keycode").asInt
                    inputController.pressKey(keyCode)
                }
                "keydown" -> {
                    val keyCode = json.get("keycode").asInt
                    inputController.pressKey(keyCode)
                }
                "keyup" -> {
                    val keyCode = json.get("keycode").asInt
                    inputController.releaseKey(keyCode)
                }
                "text" -> {
                    val text = json.get("text").asString
                    inputController.typeText(text)
                }
                else -> false
            }

            val response = JsonObject()
            response.addProperty("success", success)
            response.addProperty("command", command)
            response.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $jsonString", e)
            val response = JsonObject()
            response.addProperty("success", false)
            response.addProperty("error", e.message)
            response.toString()
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            Log.d(TAG, "Server socket closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
    }
}
