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
    private var fallbackController: InputController? = null
    private var serverThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        // Always create fallback controller
        fallbackController = RootInputController()
        startForegroundService()
        startOverlay()
        // Schedule keep-alive job to ensure service restarts if killed
        KeepAliveJobService.scheduleKeepAlive(this)
    }

    private fun startOverlay() {
        try {
            val overlayIntent = Intent(this, OverlayService::class.java)
            startService(overlayIntent)
            Log.d(TAG, "Overlay service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay service", e)
        }
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
                // Create a lazy controller that checks for AccessibilityService on each command
                val controller = LazyInputController(fallbackController!!)
                serverSocket = ServerSocket(port, controller)
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
        fallbackController?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private class LazyInputController(private val fallback: InputController) : InputController {
    private val tag = "LazyInputController"

    private fun getController(): InputController {
        val a11yController = ControlServerAccessibilityService.getInputController()
        Log.d(tag, "getController: a11yController=$a11yController")
        return if (a11yController != null) {
            Log.d(tag, "Using AccessibilityService for input")
            a11yController
        } else {
            Log.d(tag, "Using fallback RootInputController")
            fallback
        }
    }

    override fun moveMouse(x: Int, y: Int): Boolean {
        Log.d(tag, "moveMouse($x, $y)")
        return getController().moveMouse(x, y)
    }

    override fun clickMouse(button: com.romayengineer.controlserver.input.MouseButton): Boolean {
        Log.d(tag, "clickMouse($button)")
        return getController().clickMouse(button)
    }

    override fun clickMouse(x: Int, y: Int, button: com.romayengineer.controlserver.input.MouseButton): Boolean {
        Log.d(tag, "clickMouse($x, $y, $button)")
        return getController().clickMouse(x, y, button)
    }
    override fun pressMouse(button: com.romayengineer.controlserver.input.MouseButton): Boolean = getController().pressMouse(button)
    override fun releaseMouse(button: com.romayengineer.controlserver.input.MouseButton): Boolean = getController().releaseMouse(button)
    override fun scrollMouse(x: Int, y: Int, direction: com.romayengineer.controlserver.input.ScrollDirection, distance: Int): Boolean = getController().scrollMouse(x, y, direction, distance)
    override fun pressKey(keyCode: Int): Boolean = getController().pressKey(keyCode)
    override fun releaseKey(keyCode: Int): Boolean = getController().releaseKey(keyCode)
    override fun typeText(text: String): Boolean = getController().typeText(text)
    override fun isAvailable(): Boolean = getController().isAvailable()
    override fun shutdown() = getController().shutdown()
}
