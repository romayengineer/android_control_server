package com.romayengineer.controlserver.network

import com.google.gson.JsonObject
import com.romayengineer.controlserver.LogManager
import com.romayengineer.controlserver.input.InputController
import com.romayengineer.controlserver.input.MouseButton
import com.romayengineer.controlserver.input.ScrollDirection

data class CommandResult(
    val success: Boolean,
    val command: String,
    val error: String? = null
)

class CommandProcessor(private val inputController: InputController) {

    fun processCommand(command: JsonObject): CommandResult {
        return try {
            LogManager.d("Processing command: $command")
            val commandType = command.get("command")?.asString ?: return CommandResult(false, "unknown", "Missing command field")
            LogManager.d("Command type: $commandType")

            val success = when (commandType) {
                "mousemove" -> processMouseMove(command)
                "click" -> processClick(command)
                "scroll" -> processScroll(command)
                "keypress" -> processKeyPress(command)
                "keydown" -> processKeyDown(command)
                "keyup" -> processKeyUp(command)
                "text" -> processText(command)
                "mousedown" -> processMouseDown(command)
                "mouseup" -> processMouseUp(command)
                else -> {
                    LogManager.w("Unknown command: $commandType")
                    false
                }
            }

            LogManager.d("Command execution result: $success")
            CommandResult(success, commandType)
        } catch (e: Exception) {
            LogManager.e("Error processing command: ${e.message}", e)
            CommandResult(false, command.get("command")?.asString ?: "unknown", e.message)
        }
    }

    private fun processMouseMove(command: JsonObject): Boolean {
        return if (command.has("dx") || command.has("dy")) {
            val dx = command.get("dx")?.asInt ?: 0
            val dy = command.get("dy")?.asInt ?: 0
            val scaledDx = dx * 10
            val scaledDy = dy * 10
            LogManager.d("Executing mousemove relative: dx=$dx, dy=$dy (scaled to $scaledDx, $scaledDy)")
            inputController.moveMouseRelative(scaledDx, scaledDy)
        } else {
            val x = command.get("x")?.asInt ?: 0
            val y = command.get("y")?.asInt ?: 0
            LogManager.d("Executing mousemove absolute: $x, $y")
            inputController.moveMouse(x, y)
        }
    }

    private fun processClick(command: JsonObject): Boolean {
        val buttonStr = command.get("button")?.asString ?: "LEFT"
        val button = MouseButton.valueOf(buttonStr)

        return if (command.has("x") && command.has("y")) {
            val x = command.get("x").asInt
            val y = command.get("y").asInt
            LogManager.d("Executing click($x, $y, $buttonStr)")
            inputController.clickMouse(x, y, button)
        } else {
            LogManager.d("Executing click at current position with button $buttonStr")
            inputController.clickMouse(button)
        }
    }

    private fun processScroll(command: JsonObject): Boolean {
        val x = command.get("x")?.asInt ?: 0
        val y = command.get("y")?.asInt ?: 0
        val directionStr = command.get("direction")?.asString ?: "DOWN"
        val direction = ScrollDirection.valueOf(directionStr)
        val distance = command.get("distance")?.asInt ?: 3
        LogManager.d("Executing scroll($x, $y, $directionStr, $distance)")
        return inputController.scrollMouse(x, y, direction, distance)
    }

    private fun processKeyPress(command: JsonObject): Boolean {
        val keycode = command.get("keycode")?.asInt ?: return false
        LogManager.d("Executing keypress($keycode)")
        return inputController.pressKey(keycode)
    }

    private fun processKeyDown(command: JsonObject): Boolean {
        val keycode = command.get("keycode")?.asInt ?: return false
        LogManager.d("Executing keydown($keycode)")
        return inputController.pressKey(keycode)
    }

    private fun processKeyUp(command: JsonObject): Boolean {
        val keycode = command.get("keycode")?.asInt ?: return false
        LogManager.d("Executing keyup($keycode)")
        return inputController.releaseKey(keycode)
    }

    private fun processText(command: JsonObject): Boolean {
        val text = command.get("text")?.asString ?: return false
        LogManager.d("Executing typeText($text)")
        return inputController.typeText(text)
    }

    private fun processMouseDown(command: JsonObject): Boolean {
        val button = MouseButton.valueOf(command.get("button")?.asString ?: "LEFT")
        LogManager.d("Executing mousedown($button)")
        return inputController.pressMouse(button)
    }

    private fun processMouseUp(command: JsonObject): Boolean {
        val button = MouseButton.valueOf(command.get("button")?.asString ?: "LEFT")
        LogManager.d("Executing mouseup($button)")
        return inputController.releaseMouse(button)
    }
}
