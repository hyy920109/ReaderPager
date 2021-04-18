package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.os.StrictMode
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.OverScroller
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.children
import androidx.core.view.isGone

/**
 *Create by hyy on 2021/4/16
 */
class CustomScrollView constructor(context: Context, attributeSet: AttributeSet?)
    : FrameLayout(context, attributeSet) {

    private val touchSlop: Int
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private val overFlingDistance: Int
    private val overScrollDistance: Int
    private val scroller: OverScroller

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomScrollView)
        typedArray.run {
            orientation = getInteger(
                R.styleable.CustomScrollView_csl_orientation,
                ORIENTATION_HORIZONTAL
            )
        }
        typedArray.recycle()
        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
        minimumVelocity = configuration.scaledMinimumFlingVelocity
        maximumVelocity = configuration.scaledMaximumFlingVelocity
        overScrollDistance = configuration.scaledOverscrollDistance
        overFlingDistance = configuration.scaledOverflingDistance
        scroller = OverScroller(context)
    }

    private var orientation: Int = ORIENTATION_HORIZONTAL

    private val gestureDetector= GestureDetectorCompat(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                println("$TAG onDown-->e--->${e?.x}")
                return true
            }

            override fun onShowPress(e: MotionEvent?) {

            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {

                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val canScroll = ((scrollX <= 0 && distanceX < 0) ||
                        (scrollX >= getScrollRange() && distanceX > 0)).not()
                if (canScroll) {
                    scrollBy((distanceX).toInt(), 0)
                }
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
            }

            //源码中 GestureDetector已经在action_up中处理了 并且已经计算结果已经考虑了相关
            //minimumVelocity  maximumVelocity
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (velocityX < 0f) {
                    Toast.makeText(context, "Fling to left", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Fling to right", Toast.LENGTH_SHORT).show();
                }
                val canFling = ((scrollX <= 0 && velocityX > 0) ||
                        (scrollX >= getScrollRange() && velocityX < 0)).not()
                println("$TAG onFling-->canFling -->$canFling")
                if (canFling) {
                    fling(velocityX.toInt())
                }
                return true
            }

        })

    fun fling(velocityX: Int) {
        if (childCount > 0) {
            val width: Int = width - paddingLeft - paddingRight
            val bottom = getChildAt(0).width
            scroller.fling(
                scrollX, scrollY, velocityX, 0, 0, getScrollRange(), 0,
                0, width/2, 0
            )
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX
            val scrollX = scrollX
            if (scrollX != currX) {
//                overScrollBy(currX - scrollX, 0, scrollX, scrollY, getScrollRange(), 0, 0, overScrollDistance, false)
                scrollBy(currX - scrollX, 0)
            }
        }
    }
    private fun getScrollRange(): Int {
        var scrollRange = 0
        if (childCount > 0) {
            val child = getChildAt(0)
            scrollRange = maxOf(0, child.measuredWidth - (width - paddingBottom - paddingTop))
        }
        return scrollRange
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
//        println("${ScrollableViewGroup.TAG} lastX--->$lastX")
//        event?.run {
//
//            when (this.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    println("${ScrollableViewGroup.TAG} onTouchEvent ACTION_DOWN")
//                    lastX = this.x
//                    lastY = this.y
//
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    if (lastX == -1f) {
//                        lastX = this.x
//                        lastY = this.y
//                    }else {
//                        println("${ScrollableViewGroup.TAG} x--->$x")
//
//                        val dx = (this.x - lastX).toInt()
//                        val dy = (this.y - lastY).toInt()
//                        println("${ScrollableViewGroup.TAG} onTouchEvent ACTION_MOVE $dx")
//                        scrollBy(-dx, 0)
//                    }
//                }
//            }
//            lastX = this.x
//            lastY = this.y
//        }
        return gestureDetector.onTouchEvent(event)
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

    companion object {
        const val TAG = "CustomScrollView"
        const val ORIENTATION_HORIZONTAL = 0
        const val ORIENTATION_VERTICAL = 1
    }
}