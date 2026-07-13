package com.romayengineer.controlserver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
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
        private const val CHANNEL_ID = "WiFiMouseChannel"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WiFi Mouse Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi Mouse Server")
            .setContentText("Server is running on port $DEFAULT_PORT")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startServer(port: Int) {
        // Stop existing server if running
        if (serverThread?.isAlive == true) {
            Log.d(TAG, "Stopping existing server")
            serverSocket?.stop()
            serverThread?.interrupt()
            try {
                serverThread?.join(1000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while waiting for server thread", e)
            }
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
}
