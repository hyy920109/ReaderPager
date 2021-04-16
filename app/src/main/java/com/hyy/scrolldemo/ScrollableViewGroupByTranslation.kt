package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.abs

/**
 *Create by hyy on 2021/4/14
 * 使view滚动的时候
 * 如果坐标系没发生改变 那么每次lastX lastY都需要更新 然后计算delta值
 * 如果坐标系发生改变  那么每次lastX lastY只需要在down的时候复制 然后move里面计算delta值
 * 使坐标系发生改变的方法 一般有 layout offsetLeftAndRight offsetTopAndBottom
 * 方法scrollTo scrollBy 但是这俩方法都是滚动的内容 或者说是画布。
 * ViewGroup的scrollTo 和scrollBy 会导致子view(内容)的坐标系发生改变 儿自己的坐标系不发生改变
 * View的scrollTo 和scrollBy方法会导致里面的内容发生滑动
 */
class ScrollableViewGroupByTranslation constructor(context: Context, attributeSet: AttributeSet?) :
    LinearLayout(context, attributeSet) {
    private var lastX = -1f
    private var lastY = -1f
    private var downX = 0f
    private var downY = 0f

    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        println("$TAG onInterceptTouchEvent ")
        event?.run {
            when (this.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = this.x
                    downY = this.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val abX = abs(x - downX)
                    if (abX > touchSlop) {
                        println("$TAG horizontal scrolled-->")
                        return true
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        println("$TAG lastX--->$lastX")
        event?.run {

            var res = false
            when (this.action) {
                MotionEvent.ACTION_DOWN -> {
                    println("$TAG onTouchEvent ACTION_DOWN")
                    lastX = this.x
                    lastY = this.y

                }
                MotionEvent.ACTION_MOVE -> {
                    if (lastX == -1f) {
                        lastX = this.x
                        lastY = this.y
                    }else {
                        println("$TAG x--->$x")

                        val dx = (this.x - lastX).toInt()
                        val dy = (this.y - lastY).toInt()
                        println("$TAG onTouchEvent ACTION_MOVE $dx")
                        scrollBy(-dx, -dy)
                    }
                    res = true
                }
            }
            lastX = this.x
            lastY = this.y
            return res
        }
        return super.onTouchEvent(event)
    }
    companion object {
        const val TAG = "ScrollableViewGroup"
    }
}