package com.example

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

class AppIconDrawable : Drawable() {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700") // Gold color
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    
    private val text = "برعي"
    
    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        
        // Draw black background
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        
        // Draw gold Arabic text centered
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val x = width / 2
        val y = height / 2 - textBounds.exactCenterY()
        canvas.drawText(text, x, y, paint)
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        backgroundPaint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
        backgroundPaint.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
}
