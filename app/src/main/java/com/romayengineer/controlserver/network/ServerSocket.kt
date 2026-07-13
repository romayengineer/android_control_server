package com.romayengineer.controlserver.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romayengineer.controlserver.LogManager
import com.romayengineer.controlserver.input.InputController
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
    private val commandProcessor = CommandProcessor(inputController)

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
            val json = JsonParser.parseString(jsonString).asJsonObject
            val result = commandProcessor.processCommand(json)

            val response = JsonObject().apply {
                addProperty("success", result.success)
                addProperty("command", result.command)
                if (result.error != null) {
                    addProperty("error", result.error)
                }
            }
            response.toString()
        } catch (e: Exception) {
            LogManager.e("Error processing command: $jsonString - ${e.message}", e)
            val response = JsonObject().apply {
                addProperty("success", false)
                addProperty("error", e.message)
            }
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
