package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.core.view.isGone

/**
 *Create by hyy on 2021/4/16
 */
class CustomScrollView constructor(context: Context, attributeSet: AttributeSet?)
    : FrameLayout(context, attributeSet) {

    private var lastX = -1f
    private var lastY = -1f
    private var downX = 0f
    private var downY = 0f

    companion object {
        const val TAG = "CustomScrollView"
        const val ORIENTATION_HORIZONTAL = 0
        const val ORIENTATION_VERTICAL = 1
    }
    private var orientation: Int = ORIENTATION_HORIZONTAL

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomScrollView)
        typedArray.run {
            orientation = getInteger(R.styleable.CustomScrollView_csl_orientation, ORIENTATION_HORIZONTAL)
        }
        typedArray.recycle()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        println("${ScrollableViewGroup.TAG} lastX--->$lastX")
        event?.run {

            when (this.action) {
                MotionEvent.ACTION_DOWN -> {
                    println("${ScrollableViewGroup.TAG} onTouchEvent ACTION_DOWN")
                    lastX = this.x
                    lastY = this.y

                }
                MotionEvent.ACTION_MOVE -> {
                    if (lastX == -1f) {
                        lastX = this.x
                        lastY = this.y
                    }else {
                        println("${ScrollableViewGroup.TAG} x--->$x")

                        val dx = (this.x - lastX).toInt()
                        val dy = (this.y - lastY).toInt()
                        println("${ScrollableViewGroup.TAG} onTouchEvent ACTION_MOVE $dx")
                        scrollBy(-dx, 0)
                    }
                }
            }
            lastX = this.x
            lastY = this.y
        }
        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        when(orientation) {
            ORIENTATION_HORIZONTAL -> {
                layoutChildrenHorizontal(l, t, r, b)
            }
            ORIENTATION_VERTICAL -> {

            }
        }

    }

    private fun layoutChildrenHorizontal(left: Int, top: Int, right: Int, bottom: Int) {
        val parentLeft = paddingLeft

        val parentTop = paddingTop
        var curLeft = parentLeft
        children.forEachIndexed { index, view ->
            val childWidth = view.measuredWidth
            val childHeight = view.measuredHeight
            if (view.isGone.not()) {
                setChildFrame(view, curLeft, parentTop, childWidth, childHeight)
                curLeft += childWidth
            }
        }
    }

    private fun setChildFrame(child: View, left: Int, top: Int, width: Int, height: Int) {
        child.layout(left, top, left + width, top + height)
    }
}