package com.romayengineer.controlserver

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionChecker {
    private const val TAG = "PermissionChecker"

    data class PermissionStatus(
        val isGranted: Boolean,
        val missingPermissions: List<String> = emptyList(),
        val details: String = ""
    )

    fun checkPermissions(context: Context): PermissionStatus {
        val requiredPermissions = listOf(
            "android.permission.INTERNET",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )

        val missingPermissions = mutableListOf<String>()
        val details = mutableListOf<String>()

        for (permission in requiredPermissions) {
            val status = ContextCompat.checkSelfPermission(context, permission)
            val permissionName = permission.substringAfterLast(".")

            if (status != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
                details.add("✗ $permissionName")
                LogManager.w("Missing permission: $permissionName")
            } else {
                details.add("✓ $permissionName")
                LogManager.d("Permission granted: $permissionName")
            }
        }

        val isGranted = missingPermissions.isEmpty()

        LogManager.d(if (isGranted) "All permissions granted!" else "Missing ${missingPermissions.size} permission(s)")

        return PermissionStatus(
            isGranted = isGranted,
            missingPermissions = missingPermissions,
            details = details.joinToString("\n")
        )
    }

    fun checkAccessibilityService(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val isEnabled = enabledServices.contains("com.romayengineer.controlserver/.service.ControlServerAccessibilityService")
            LogManager.d("AccessibilityService enabled: $isEnabled")
            isEnabled
        } catch (e: Exception) {
            LogManager.e("Error checking accessibility service: ${e.message}", e)
            false
        }
    }

    fun runStartupCheck(context: Context): StartupStatus {
        LogManager.i("Running startup permission check...")

        val permissions = checkPermissions(context)
        val accessibility = checkAccessibilityService(context)

        val status = StartupStatus(
            permissionsOk = permissions.isGranted,
            accessibilityEnabled = accessibility,
            details = buildString {
                append("=== Startup Check ===\n")
                append("Permissions: ${if (permissions.isGranted) "✓ OK" else "✗ FAILED"}\n")
                append(permissions.details)
                append("\n\nAccessibilityService: ${if (accessibility) "✓ Enabled" else "✗ Disabled"}")
            }
        )

        LogManager.i("Startup check complete: ${if (status.isReady) "READY" else "NOT READY"}")
        return status
    }

    data class StartupStatus(
        val permissionsOk: Boolean,
        val accessibilityEnabled: Boolean,
        val details: String
    ) {
        val isReady: Boolean
            get() = permissionsOk && accessibilityEnabled

        val statusColor: Int
            get() = if (isReady) android.graphics.Color.GREEN else android.graphics.Color.RED
    }
}
