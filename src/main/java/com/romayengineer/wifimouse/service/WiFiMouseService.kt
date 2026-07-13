package com.romayengineer.controlserver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.romayengineer.controlserver.R
import com.romayengineer.controlserver.input.InputController
import com.romayengineer.controlserver.input.RootInputController
import com.romayengineer.controlserver.network.ServerSocket
import kotlin.concurrent.thread

class WiFiMouseService : Service() {
    companion object {
        private const val TAG = "WiFiMouseService"
        private const val NOTIFICATION_ID = 1
        const val DEFAULT_PORT = 3934
        const val KEY_PORT = "port"
    }

    private var serverSocket: ServerSocket? = null
    private var inputController: InputController? = null
    private var serverThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        inputController = RootInputController()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        val port = intent?.getIntExtra(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        startServer(port)
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi Mouse Server")
            .setContentText("Server is running on port $DEFAULT_PORT")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startServer(port: Int) {
        if (serverThread?.isAlive == true) {
            Log.d(TAG, "Server already running")
            return
        }

        serverThread = thread(isDaemon = false) {
            try {
                serverSocket = ServerSocket(port, inputController!!)
                serverSocket?.start()
                Log.d(TAG, "Server started on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        serverSocket?.stop()
        inputController?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "WiFiMouseChannel"
    }
}
