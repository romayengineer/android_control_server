package com.romayengineer.controlserver.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.romayengineer.controlserver.input.AccessibilityInputController

class ControlServerAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "ControlServerA11yService"
        var instance: ControlServerAccessibilityService? = null
            private set

        fun getInputController(): AccessibilityInputController? {
            return instance?.inputController
        }
    }

    private lateinit var inputController: AccessibilityInputController

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
        inputController = AccessibilityInputController(this)
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle events, just provide input injection capability
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroyed")
        instance = null
        super.onDestroy()
    }
}
