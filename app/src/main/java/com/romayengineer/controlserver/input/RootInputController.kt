package com.romayengineer.controlserver.input

import android.util.Log

class RootInputController : InputController {
    companion object {
        private const val TAG = "RootInputController"
    }

    private fun executeCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            false
        }
    }

    override fun moveMouse(x: Int, y: Int): Boolean {
        val command = "input mousemove $x $y"
        return executeCommand(command)
    }

    override fun clickMouse(button: MouseButton): Boolean {
        val buttonCode = when (button) {
            MouseButton.LEFT -> 1
            MouseButton.RIGHT -> 2
            MouseButton.MIDDLE -> 4
        }
        val command = "input mouse tap $buttonCode"
        return executeCommand(command)
    }

    override fun pressMouse(button: MouseButton): Boolean {
        val buttonCode = when (button) {
            MouseButton.LEFT -> 1
            MouseButton.RIGHT -> 2
            MouseButton.MIDDLE -> 4
        }
        val command = "input mouse down $buttonCode"
        return executeCommand(command)
    }

    override fun releaseMouse(button: MouseButton): Boolean {
        val buttonCode = when (button) {
            MouseButton.LEFT -> 1
            MouseButton.RIGHT -> 2
            MouseButton.MIDDLE -> 4
        }
        val command = "input mouse up $buttonCode"
        return executeCommand(command)
    }

    override fun scrollMouse(x: Int, y: Int, direction: ScrollDirection, distance: Int): Boolean {
        val (dx, dy) = when (direction) {
            ScrollDirection.UP -> Pair(0, -distance)
            ScrollDirection.DOWN -> Pair(0, distance)
            ScrollDirection.LEFT -> Pair(-distance, 0)
            ScrollDirection.RIGHT -> Pair(distance, 0)
        }
        val command = "input scroll $x $y $dx $dy"
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
