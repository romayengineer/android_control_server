package com.romayengineer.controlserver

import android.content.Intent
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.romayengineer.controlserver.service.WiFiMouseService
import com.romayengineer.controlserver.ui.CursorView
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    companion object {
        private var cursorView: CursorView? = null
        private val mainHandler = Handler(Looper.getMainLooper())
        private const val PERMISSION_REQUEST_CODE = 1001

        fun updateCursorPosition(x: Int, y: Int) {
            mainHandler.post {
                cursorView?.updateCursorPosition(x, y)
            }
        }

        fun setCursorVisible(visible: Boolean) {
            mainHandler.post {
                cursorView?.setCursorVisible(visible)
            }
        }
    }

    private var showingPermissionDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val portEditText = findViewById<EditText>(R.id.port_input)
        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val statusText = findViewById<TextView>(R.id.status_text)
        val ipAddressText = findViewById<TextView>(R.id.ip_address_text)
        val logText = findViewById<TextView>(R.id.log_text)
        val statusBadge = findViewById<View>(R.id.status_badge)

        cursorView = findViewById<CursorView>(R.id.cursor_view)

        LogManager.setLogTextView(logText)
        LogManager.i("App started")

        checkPermissionsAndShowDialogs(statusBadge)

        portEditText.setText(WiFiMouseService.DEFAULT_PORT.toString())
        displayLocalIpAddress(ipAddressText)

        setupButtonListeners()

        // Auto-start the server
        startServer(WiFiMouseService.DEFAULT_PORT, statusText)
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when app comes back into focus (after user returns from Settings)
        val statusBadge = findViewById<View>(R.id.status_badge)
        checkPermissionsAndShowDialogs(statusBadge)
    }

    private fun checkPermissionsAndShowDialogs(statusBadge: View) {
        // Run startup permission check
        val startupStatus = PermissionChecker.runStartupCheck(this)
        updateStatusBadge(statusBadge, startupStatus)

        // Check if SYSTEM_ALERT_WINDOW permission is missing
        val overlayPermissionMissing = !hasOverlayPermission()

        // Only show dialogs if not already showing one
        if (!showingPermissionDialog) {
            // Show overlay permission dialog if missing
            if (overlayPermissionMissing) {
                showingPermissionDialog = true
                showOverlayPermissionDialog()
            } else if (!startupStatus.permissionsOk) {
                // Show standard permissions request if overlay permission is OK but others are missing
                showingPermissionDialog = true
                requestMissingPermissions()
            } else if (!startupStatus.accessibilityEnabled) {
                // Only show accessibility dialog if permissions are already OK
                showingPermissionDialog = true
                showAccessibilityDialog()
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            ContextCompat.checkSelfPermission(this, "android.permission.SYSTEM_ALERT_WINDOW") == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupButtonListeners() {
        val portEditText = findViewById<EditText>(R.id.port_input)
        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val statusText = findViewById<TextView>(R.id.status_text)

        startButton.setOnClickListener {
            val port = portEditText.text.toString().toIntOrNull() ?: WiFiMouseService.DEFAULT_PORT
            startServer(port, statusText)
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, WiFiMouseService::class.java)
            stopService(serviceIntent)
            statusText.text = "Server stopped"
        }
    }

    private fun startServer(port: Int, statusText: TextView) {
        val serviceIntent = Intent(this, WiFiMouseService::class.java)
        serviceIntent.putExtra(WiFiMouseService.KEY_PORT, port)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        statusText.text = "Server started on port $port"
    }

    private fun displayLocalIpAddress(textView: TextView) {
        val ipAddress = getLocalIpAddress()
        textView.text = ipAddress ?: "Unable to get IP"
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .find { it is Inet4Address && !it.isLoopbackAddress && it.hostAddress?.isNotEmpty() == true }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun updateStatusBadge(badge: View, status: PermissionChecker.StartupStatus) {
        val color = if (status.isReady) {
            LogManager.d("Startup check PASSED - All permissions OK")
            android.graphics.Color.GREEN
        } else {
            LogManager.w("Startup check FAILED - Missing permissions or AccessibilityService")
            android.graphics.Color.RED
        }

        badge.background?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun requestMissingPermissions() {
        // Note: SYSTEM_ALERT_WINDOW cannot be requested via standard permission dialog
        // It's a special permission handled separately in showOverlayPermissionDialog()
        val requiredPermissions = arrayOf(
            "android.permission.INTERNET",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.FOREGROUND_SERVICE"
        )
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            LogManager.i("Requesting ${missingPermissions.size} missing permission(s)")
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE)
        } else {
            LogManager.i("All standard permissions already granted")
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Display Over Other Apps")
            .setMessage("The app needs permission to display the cursor overlay across all apps.\n\nPlease go to Settings → Apps → Special app access → Display over other apps and enable this app.")
            .setPositiveButton("Open Settings") { _, _ ->
                LogManager.i("User requested to open overlay permission settings")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(intent)
            }
            .setNegativeButton("Skip") { _, _ ->
                showingPermissionDialog = false
            }
            .setOnDismissListener {
                showingPermissionDialog = false
            }
            .show()
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable AccessibilityService")
            .setMessage("AccessibilityService is not enabled. The app works best with AccessibilityService enabled for click and scroll operations.\n\nWould you like to open Accessibility settings?")
            .setPositiveButton("Open Settings") { _, _ ->
                LogManager.i("User requested to open Accessibility settings")
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Skip") { _, _ ->
                showingPermissionDialog = false
            }
            .setOnDismissListener {
                showingPermissionDialog = false
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        showingPermissionDialog = false
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (granted) {
                LogManager.i("All requested permissions granted!")
                // Re-run startup check to update badge
                val statusBadge = findViewById<View>(R.id.status_badge)
                val startupStatus = PermissionChecker.runStartupCheck(this)
                updateStatusBadge(statusBadge, startupStatus)

                // Now show accessibility dialog if accessibility is not enabled
                if (!startupStatus.accessibilityEnabled) {
                    showingPermissionDialog = true
                    showAccessibilityDialog()
                }
            } else {
                LogManager.w("Some permissions were denied")
            }
        }
    }
}
