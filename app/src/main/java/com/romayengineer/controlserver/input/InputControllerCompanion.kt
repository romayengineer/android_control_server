package com.romayengineer.controlserver.input

import android.util.Log

object InputControllerCompanion {
    private const val TAG = "InputControllerCompanion"
    var screenWidth: Int = 1080
    var screenHeight: Int = 1920

    fun setScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        Log.d(TAG, "Screen dimensions set to: $width x $height")
    }

    fun clampCoordinates(x: Int, y: Int): Pair<Int, Int> {
        val clampedX = x.coerceIn(0, screenWidth)
        val clampedY = y.coerceIn(0, screenHeight)
        return Pair(clampedX, clampedY)
    }
}
