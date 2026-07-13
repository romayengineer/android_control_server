package com.romayengineer.controlserver.service

import android.app.ActivityManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class KeepAliveJobService : JobService() {
    companion object {
        private const val TAG = "KeepAliveJobService"
        const val JOB_ID = 1001

        fun scheduleKeepAlive(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? android.app.job.JobScheduler
            if (jobScheduler != null) {
                val jobInfo = android.app.job.JobInfo.Builder(
                    JOB_ID,
                    android.content.ComponentName(context, KeepAliveJobService::class.java)
                )
                    .setPeriodic(15 * 60 * 1000) // 15 minutes
                    .setRequiresDeviceIdle(false)
                    .setPersisted(true)
                    .build()

                jobScheduler.schedule(jobInfo)
                Log.d(TAG, "Keep-alive job scheduled")
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Keep-alive job triggered")
        ensureServiceRunning()
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    private fun ensureServiceRunning() {
        if (!isServiceRunning(WiFiMouseService::class.java)) {
            Log.d(TAG, "WiFiMouseService not running, restarting...")
            val serviceIntent = Intent(this, WiFiMouseService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        if (!isServiceRunning(OverlayService::class.java)) {
            Log.d(TAG, "OverlayService not running, restarting...")
            val overlayIntent = Intent(this, OverlayService::class.java)
            startService(overlayIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
