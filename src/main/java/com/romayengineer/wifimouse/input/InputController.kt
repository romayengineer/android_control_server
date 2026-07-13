package com.romayengineer.controlserver.input

interface InputController {
    fun moveMouse(x: Int, y: Int): Boolean
    fun clickMouse(button: MouseButton = MouseButton.LEFT): Boolean
    fun pressMouse(button: MouseButton = MouseButton.LEFT): Boolean
    fun releaseMouse(button: MouseButton = MouseButton.LEFT): Boolean
    fun scrollMouse(x: Int, y: Int, direction: ScrollDirection, distance: Int): Boolean
    fun pressKey(keyCode: Int): Boolean
    fun releaseKey(keyCode: Int): Boolean
    fun typeText(text: String): Boolean
    fun isAvailable(): Boolean
    fun shutdown()
}

enum class MouseButton {
    LEFT, RIGHT, MIDDLE
}

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}
