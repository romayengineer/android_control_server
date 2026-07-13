package com.romayengineer.controlserver.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX = 0f
    private var cursorY = 0f
    private var showCursor = true
    private var animator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint().apply {
        color = Color.argb(50, 255, 0, 0)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun updateCursorPosition(x: Int, y: Int) {
        mainHandler.post {
            val targetX = x.toFloat()
            val targetY = y.toFloat()
            val startX = cursorX
            val startY = cursorY

            animator?.cancel()

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 150
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    cursorX = startX + (targetX - startX) * progress
                    cursorY = startY + (targetY - startY) * progress
                    postInvalidate()
                }
                start()
            }
        }
    }

    fun setCursorVisible(visible: Boolean) {
        showCursor = visible
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showCursor) return

        val cursorSize = 24f

        // Draw outer circle
        canvas.drawCircle(cursorX, cursorY, cursorSize, paint)

        // Draw filled inner circle
        canvas.drawCircle(cursorX, cursorY, cursorSize / 2, fillPaint)

        // Draw crosshair
        val lineLength = cursorSize + 8f
        canvas.drawLine(cursorX - lineLength, cursorY, cursorX + lineLength, cursorY, paint)
        canvas.drawLine(cursorX, cursorY - lineLength, cursorX, cursorY + lineLength, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Don't consume touch events - allow them to pass through to underlying apps
        return false
    }
}
