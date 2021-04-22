package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView

/**
 *Create by hyy on 2021/4/14
 */
class ScrollableView constructor(context: Context, attributeSet: AttributeSet?) :
    View(context, attributeSet) {
    private var lastX = 0f
    private var lastY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
    }

    //canvas from parent
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        println("$TAG  onLayout")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        println("$TAG  onTouchEvent")
        parent.requestDisallowInterceptTouchEvent(true)
        event?.run {
            when (this.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = this.x
                    lastY = this.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (this.x - lastX).toInt()
                    val dy = (this.y - lastY).toInt()

                    translationX += dx.toFloat()
                    translationY += dy.toFloat()
//                    layout(left + dx, top + dy, right + dx, (bottom + dy))
//                    offsetLeftAndRight(dx)
//                    offsetTopAndBottom(dy)
//                    (parent as ViewGroup).scrollBy(-dx, -dy)

                }
                MotionEvent.ACTION_CANCEL -> {
                    println("$TAG onCancel-->")
                    return false
                }
                MotionEvent.ACTION_UP -> {
                }
            }
//            lastX = this.x
//            lastY = this.y
            return true
        }
        return super.onTouchEvent(event)
    }

    companion object {
        const val TAG = "ScrollableView"
    }
}