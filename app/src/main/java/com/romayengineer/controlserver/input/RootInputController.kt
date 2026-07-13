package com.romayengineer.controlserver.input

import android.util.Log
import com.romayengineer.controlserver.service.OverlayService

class RootInputController : InputController {
    companion object {
        private const val TAG = "RootInputController"
    }

    private fun executeCommand(command: String): Boolean {
        return try {
            Log.d(TAG, "Executing shell command: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            val success = exitCode == 0
            Log.d(TAG, "Command executed with exit code: $exitCode (success: $success)")

            // Log any error output
            if (!success) {
                val errorStream = process.errorStream.bufferedReader().readText()
                if (errorStream.isNotEmpty()) {
                    Log.e(TAG, "Command error output: $errorStream")
                }
                val outputStream = process.inputStream.bufferedReader().readText()
                if (outputStream.isNotEmpty()) {
                    Log.e(TAG, "Command output: $outputStream")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            false
        }
    }

    override fun moveMouse(x: Int, y: Int): Boolean {
        val command = "input mousemove $x $y"
        Log.d(TAG, "moveMouse($x, $y)")
        OverlayService.updateCursorPosition(x, y)
        return executeCommand(command)
    }

    override fun clickMouse(button: MouseButton): Boolean {
        // Default click at center - x and y should come from moveMouse first
        val command = "input tap 500 500"
        Log.d(TAG, "clickMouse($button) at default position")
        return executeCommand(command)
    }

    override fun clickMouse(x: Int, y: Int, button: MouseButton): Boolean {
        // First move cursor to the position
        moveMouse(x, y)
        // Then perform the click
        val command = "input swipe $x $y $x $y 100"
        Log.d(TAG, "clickMouse($x, $y, $button) - moving to position then clicking")
        return executeCommand(command)
    }

    override fun pressMouse(button: MouseButton): Boolean {
        // Mouse press not directly supported via input command
        // Use tap as alternative
        Log.d(TAG, "pressMouse($button) - not supported, using tap instead")
        return clickMouse(button)
    }

    override fun releaseMouse(button: MouseButton): Boolean {
        // Mouse release not directly supported via input command
        Log.d(TAG, "releaseMouse($button) - not directly supported")
        return true
    }

    override fun scrollMouse(x: Int, y: Int, direction: ScrollDirection, distance: Int): Boolean {
        // First move cursor to the position
        moveMouse(x, y)
        // Then perform the scroll
        val (dx, dy) = when (direction) {
            ScrollDirection.UP -> Pair(0, -distance)
            ScrollDirection.DOWN -> Pair(0, distance)
            ScrollDirection.LEFT -> Pair(-distance, 0)
            ScrollDirection.RIGHT -> Pair(distance, 0)
        }
        val command = "input scroll $x $y $dx $dy"
        Log.d(TAG, "scrollMouse($x, $y, $direction, $distance) - moving to position then scrolling")
        return executeCommand(command)
    }

    override fun pressKey(keyCode: Int): Boolean {
        val command = "input keyevent $keyCode"
        return executeCommand(command)
    }

    override fun releaseKey(keyCode: Int): Boolean {
        // Most key events in Android are instantaneous, no need for separate release
        return true
    }

    override fun typeText(text: String): Boolean {
        val command = "input text \"$text\""
        return executeCommand(command)
    }

    override fun isAvailable(): Boolean {
        return executeCommand("input --help > /dev/null 2>&1")
    }

    override fun shutdown() {
        // Nothing to clean up for root controller
    }
}
