package com.example.fomofocus

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bgPaint = Paint().apply {
        color = "#BDBDBD".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 16f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint().apply {
        color = "#3F51B5".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private var progressAngle = 0f

    fun setProgress(percent: Int) {
        progressAngle = (percent / 100f) * 360f
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 40f
        val rect = RectF(
            padding,
            padding,
            width - padding,
            height - padding
        )

        // garis background
        canvas.drawArc(rect, -90f, 360f, false, bgPaint)

        // garis progress
        canvas.drawArc(rect, -90f, progressAngle, false, progressPaint)
    }
}
