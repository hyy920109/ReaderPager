package com.hyy.scrolldemo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.widget.NestedScrollView
import kotlin.math.max

/**
 *Create by hyy on 2021/4/19
 */
class UnspecifiedFrameLayout constructor(context: Context, attributeSet: AttributeSet?) :
    ViewGroup(context, attributeSet) {

    companion object {
        const val TAG = "UnspecifiedFrameLayout"
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val parentLeft = paddingLeft

        val parentTop = paddingTop
        children.forEachIndexed { index, view ->
            val childWidth = view.measuredWidth
            val childHeight = view.measuredHeight
            if (view.isGone.not()) {
                view.layout(parentLeft, parentTop, parentLeft + childWidth, parentTop + childHeight)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //这个传起来的widthMeasureSpec 和heightMeasureSpec 是decorView的
        //默认不重写这个的化会导致
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        println("$TAG  widthMode-->${widthMeasureSpec.getMeasureMode()}")
        println("$TAG  widthSize-->${widthMeasureSpec.getMeasureSize()}")
        println("$TAG  heightMode-->${heightMeasureSpec.getMeasureMode()}")
        println("$TAG  heightSize-->${heightMeasureSpec.getMeasureSize()}")
        var maxWidth = 0
        var maxHeight = 0
        var childState = 0
        children.forEach { child ->
            if (child.isGone.not()) {
                //测量child的
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                println("child.measuredHeight-->${child.measuredHeight}")

                maxWidth = max(maxWidth, child.measuredWidth)
                maxHeight = max(maxHeight, child.measuredHeight)
                childState = combineMeasuredStates(childState, child.measuredState)
            }
        }

        //consider padding
        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom

        maxWidth = max(maxWidth, suggestedMinimumWidth)
        maxHeight = max(maxHeight, suggestedMinimumHeight)

        println("maxHeight-->$maxHeight")
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            getSelfSize(maxHeight, heightMeasureSpec)
        )
        println("CustomFrameLayout height--> $measuredHeight")
    }

    override fun measureChild(
        child: View?,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        println("measureChild-->")
        val lp = child!!.layoutParams

        val childWidthMeasureSpec = getChildMeasureSpec(
            parentWidthMeasureSpec,
            paddingLeft + paddingRight, lp.width
        )
        val usedTotal: Int = (paddingTop + paddingBottom)
        //可以通过设置child的MeasureSpecMode为UNSPECIFIED让子view高度不受限制
        //借鉴了 ScrollView 和NestScrollView
        // ScrollView 和NestScrollView 还重写了
        val childHeightMeasureSpec: Int = MeasureSpec.makeMeasureSpec(
            max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal),
            MeasureSpec.UNSPECIFIED
        )

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun measureChildWithMargins(
        child: View?,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {
        super.measureChildWithMargins(
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }

    //仿照 view.resolveSizeAndState写的
    private fun getSelfSize(size: Int, spec: Int): Int {
        val mode = MeasureSpec.getMode(spec)
        val specSize = MeasureSpec.getSize(spec)

        return when (mode) {
            MeasureSpec.AT_MOST -> {
                if (specSize < size) {
                    specSize or MEASURED_STATE_TOO_SMALL
                } else {
                    size
                }
            }
            MeasureSpec.EXACTLY -> {
                specSize
            }
            else -> {
                size
            }
        }
    }
}