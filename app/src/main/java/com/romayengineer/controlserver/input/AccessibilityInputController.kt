package com.romayengineer.controlserver.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.romayengineer.controlserver.MainActivity

data class QuadCoords(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

class AccessibilityInputController(private val service: AccessibilityService) : InputController {
    companion object {
        private const val TAG = "AccessibilityInputController"
    }

    override fun moveMouse(x: Int, y: Int): Boolean {
        Log.d(TAG, "moveMouse($x, $y)")
        MainActivity.updateCursorPosition(x, y)
        return true
    }

    override fun clickMouse(button: MouseButton): Boolean {
        Log.d(TAG, "clickMouse($button) - No coordinates provided")
        return false
    }

    override fun clickMouse(x: Int, y: Int, button: MouseButton): Boolean {
        Log.d(TAG, "clickMouse($x, $y, $button)")
        return performClick(x.toFloat(), y.toFloat())
    }

    override fun pressMouse(button: MouseButton): Boolean {
        Log.d(TAG, "pressMouse($button)")
        return true // AccessibilityService doesn't support separate press/release
    }

    override fun releaseMouse(button: MouseButton): Boolean {
        Log.d(TAG, "releaseMouse($button)")
        return true
    }

    override fun scrollMouse(x: Int, y: Int, direction: ScrollDirection, distance: Int): Boolean {
        Log.d(TAG, "scrollMouse($x, $y, $direction, $distance)")
        return performScroll(x.toFloat(), y.toFloat(), direction)
    }

    override fun pressKey(keyCode: Int): Boolean {
        Log.d(TAG, "pressKey($keyCode) - Not supported by AccessibilityService")
        return false
    }

    override fun releaseKey(keyCode: Int): Boolean {
        Log.d(TAG, "releaseKey($keyCode)")
        return true
    }

    override fun typeText(text: String): Boolean {
        Log.d(TAG, "typeText($text) - Not supported by AccessibilityService")
        return false
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun shutdown() {
        Log.d(TAG, "Accessibility service shutdown")
    }

    private fun performClick(x: Float, y: Float): Boolean {
        return try {
            Log.d(TAG, "Performing click at ($x, $y)")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.w(TAG, "AccessibilityService gestures require API 24+")
                return false
            }

            val clickPath = Path().apply {
                moveTo(x, y)
                lineTo(x, y)
            }

            val gesture = GestureDescription.Builder().apply {
                addStroke(GestureDescription.StrokeDescription(clickPath, 0, 50))
            }.build()

            val success = service.dispatchGesture(gesture, null, null)
            Log.d(TAG, "Click dispatch result: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform click", e)
            false
        }
    }

    private fun performScroll(x: Float, y: Float, direction: ScrollDirection): Boolean {
        return try {
            Log.d(TAG, "Performing scroll at ($x, $y) direction=$direction")
            val coords = when (direction) {
                ScrollDirection.UP -> QuadCoords(x, y + 100, x, y - 100)
                ScrollDirection.DOWN -> QuadCoords(x, y - 100, x, y + 100)
                ScrollDirection.LEFT -> QuadCoords(x + 100, y, x - 100, y)
                ScrollDirection.RIGHT -> QuadCoords(x - 100, y, x + 100, y)
            }

            val scrollPath = Path().apply {
                moveTo(coords.startX, coords.startY)
                lineTo(coords.endX, coords.endY)
            }

            val gesture = GestureDescription.Builder().apply {
                addStroke(GestureDescription.StrokeDescription(scrollPath, 0, 500))
            }.build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val success = service.dispatchGesture(gesture, null, null)
                Log.d(TAG, "Scroll dispatch result: $success")
                success
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform scroll", e)
            false
        }
    }
}
