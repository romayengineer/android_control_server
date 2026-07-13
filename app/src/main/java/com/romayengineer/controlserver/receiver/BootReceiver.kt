package com.romayengineer.controlserver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.romayengineer.controlserver.service.KeepAliveJobService
import com.romayengineer.controlserver.service.WiFiMouseService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting WiFiMouseService")
            val serviceIntent = Intent(context, WiFiMouseService::class.java)
            if (context != null) {
                ContextCompat.startForegroundService(context, serviceIntent)
                // Schedule keep-alive job
                KeepAliveJobService.scheduleKeepAlive(context)
            }
        }
    }
}
