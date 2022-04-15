package cz.kudlav.scomview

import android.content.Context
import android.view.animation.Animation
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class Light(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()
    fun off() {
        clearAnimation()
        alpha = 0.2f
    }

    fun on() {
        clearAnimation()
        alpha = 1f
    }

    fun blink(animation: Animation?) {
        alpha = 1f
        startAnimation(animation)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(heightMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.style = Paint.Style.FILL
        canvas.translate(height / 2f, height / 2f)
        canvas.drawCircle(0f, 0f, height / 2f, paint)
    }

    init {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.Light,
                0, 0)
        var color = Color.DKGRAY
        try {
            val colorProperty = a.getString(R.styleable.Light_light_color)
            if (colorProperty != null) color = Color.parseColor(colorProperty)
        } finally {
            a.recycle()
        }
        paint.color = color
        paint.style = Paint.Style.FILL
    }
}