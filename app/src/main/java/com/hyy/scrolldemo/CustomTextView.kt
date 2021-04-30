package com.hyy.scrolldemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class CustomTextView constructor(context: Context, attributeSet: AttributeSet?) :
        AppCompatTextView(context, attributeSet) {
    private val mShadowColor = intArrayOf(0x4A000000, 0x19000000, 0x00000000)

    private val mShadowDrawable: GradientDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, mShadowColor)

    init {
        mShadowDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        mShadowDrawable.setGradientCenter(0f, 5f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        println("ReaderPager------> child onDraw$tag")
        mShadowDrawable.setBounds(measuredWidth, 0, measuredWidth+35, measuredHeight)
        mShadowDrawable.draw(canvas)
    }
}