package com.romayengineer.controlserver.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romayengineer.controlserver.LogManager
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
            LogManager.d("Server socket created on port $port")

            while (isRunning) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        LogManager.d("Client connected: ${clientSocket.inetAddress.hostAddress}")
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        LogManager.e("Error accepting client: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            LogManager.e("Server socket error: ${e.message}", e)
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
                LogManager.e("Error handling client: ${e.message}", e)
            } finally {
                clientSocket.close()
                LogManager.d("Client disconnected")
            }
        }.start()
    }

    private fun processCommand(jsonString: String): String {
        return try {
            LogManager.d("Processing command: $jsonString")
            val json = JsonParser.parseString(jsonString).asJsonObject
            val command = json.get("command").asString
            LogManager.d("Command type: $command")

            val success = when (command) {
                "mousemove" -> {
                    val x = json.get("x").asInt
                    val y = json.get("y").asInt
                    LogManager.d("Executing mousemove($x, $y)")
                    inputController.moveMouse(x, y)
                }
                "click" -> {
                    val x = json.get("x")?.asInt ?: 500
                    val y = json.get("y")?.asInt ?: 500
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    LogManager.d("Executing click($x, $y, $button)")
                    val result = inputController.clickMouse(x, y, button)
                    LogManager.d("Click result: $result")
                    result
                }
                "mousedown" -> {
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    LogManager.d("Executing mousedown($button)")
                    inputController.pressMouse(button)
                }
                "mouseup" -> {
                    val button = json.get("button")?.asString?.let { MouseButton.valueOf(it) } ?: MouseButton.LEFT
                    LogManager.d("Executing mouseup($button)")
                    inputController.releaseMouse(button)
                }
                "scroll" -> {
                    val x = json.get("x").asInt
                    val y = json.get("y").asInt
                    val direction = ScrollDirection.valueOf(json.get("direction").asString)
                    val distance = json.get("distance")?.asInt ?: 5
                    LogManager.d("Executing scroll($x, $y, $direction, $distance)")
                    inputController.scrollMouse(x, y, direction, distance)
                }
                "keypress" -> {
                    val keyCode = json.get("keycode").asInt
                    LogManager.d("Executing keypress($keyCode)")
                    inputController.pressKey(keyCode)
                }
                "keydown" -> {
                    val keyCode = json.get("keycode").asInt
                    LogManager.d("Executing keydown($keyCode)")
                    inputController.pressKey(keyCode)
                }
                "keyup" -> {
                    val keyCode = json.get("keycode").asInt
                    LogManager.d("Executing keyup($keyCode)")
                    inputController.releaseKey(keyCode)
                }
                "text" -> {
                    val text = json.get("text").asString
                    LogManager.d("Executing typeText($text)")
                    inputController.typeText(text)
                }
                else -> {
                    LogManager.w("Unknown command: $command")
                    false
                }
            }

            LogManager.d("Command execution result: $success")
            val response = JsonObject()
            response.addProperty("success", success)
            response.addProperty("command", command)
            response.toString()
        } catch (e: Exception) {
            LogManager.e("Error processing command: $jsonString - ${e.message}", e)
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
            LogManager.d("Server socket closed")
        } catch (e: Exception) {
            LogManager.e("Error closing server socket: ${e.message}", e)
        }
    }
}
