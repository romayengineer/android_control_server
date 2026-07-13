package com.romayengineer.controlserver.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import com.romayengineer.controlserver.ui.CursorView

class OverlayService : Service() {
    companion object {
        private const val TAG = "OverlayService"
        private var overlayView: CursorView? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateCursorPosition(x: Int, y: Int) {
            mainHandler.post {
                overlayView?.updateCursorPosition(x, y)
            }
        }

        fun setCursorVisible(visible: Boolean) {
            mainHandler.post {
                overlayView?.setCursorVisible(visible)
            }
        }
    }

    private var windowManager: WindowManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OverlayService started")
        createOverlay()
        return START_STICKY_COMPATIBILITY
    }

    private fun createOverlay() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            overlayView = CursorView(this).apply {
                setCursorVisible(true)
            }

            val params = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }

            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay view created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create overlay", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "OverlayService destroyed")
        try {
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
