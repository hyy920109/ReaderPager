package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReaderPager constructor(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {


    private val scroller: OverScroller = OverScroller(context, interpolator)

    private var curItemIndex = 0

    private val itemViewHolders = mutableListOf<ViewHolder>()

    private var totalScrollX = 0.0f
    private var offsetPage = 0

    //默认GestureDetectorCompat不支持单纯的action_up事件
    //解决方案可以按照直接源码的onFling不限制速度就可以了拿到up事件了
    private val gestureDetector= GestureDetectorCompat(
            context,
            object : GestureDetectorCompat.CustomOnGestureListener {
                override fun onDown(e: MotionEvent?): Boolean {
                    println("$TAG onDown-->e--->${e?.x}")
                    totalScrollX = 0.0f
                    offsetPage = 0
                    scroller.forceFinished(true)
                    return true
                }

                override fun onLongClicked(e: MotionEvent?): Boolean {
                    //需要根据情况返回true或者false 如果返回true 则代表后续其他事件都没用了


                    return false
                }

                override fun onShowPress(e: MotionEvent?) {
                }

                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    val needDeltaX = calculateTargetDeltaX(curItemIndex, scrollX)
                    println("$TAG onUp scrollX-->$scrollX")
                    println("$TAG onUp needDeltaX-->$needDeltaX")
                    scroller.startScroll(scrollX, 0, -needDeltaX, 0, 350)
                    ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                    return true
                }


                override fun onScroll(
                        e1: MotionEvent?,
                        e2: MotionEvent?,
                        distanceX: Float,
                        distanceY: Float
                ): Boolean {
                    requestDisallowInterceptTouchEvent(true)
                    val canScroll = ((scrollX + distanceX < 0) ||
                            (scrollX+ distanceX > getScrollRange())).not()

                    if (canScroll) {
                        scrollBy((distanceX).toInt(), 0)
                        totalScrollX += distanceX
                        var totalOffset = totalScrollX.toInt()/measuredWidth
                        if (totalOffset > offsetPage) {
                            curItemIndex ++
                            offsetPage = totalOffset
                        }else if (totalOffset < offsetPage){
                            offsetPage = totalOffset
                            curItemIndex --
                        }

                    }
                    println("$TAG onScroll-->$scrollX")
                    return true
                }

                override fun onLongPress(e: MotionEvent?) {
                    println("$TAG onLongPress-->")
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
                        println("Fling to left page need ++  $curItemIndex")
                        println("Fling to left page need -->${scrollX < itemViewHolders[curItemIndex].viewLeft}")
                        if (scrollX < itemViewHolders[curItemIndex].viewLeft) {
                            //这种情况是前面是往右滑动 最后抬起手指fling的时候 方向是向左的
                            //这个时候curItemIndex不需要变
                        } else {
                            curItemIndex ++
                            curItemIndex = min(childCount-1, curItemIndex)
                        }

                    } else {
                        println("Fling to right page need-- $curItemIndex")
                        if (scrollX > itemViewHolders[curItemIndex].viewLeft) {
                            //这种情况是前面是往左滑动 最后抬起手指fling的时候 方向是向右的
                            //这个时候curItemIndex不需要变
                        }else {
                            curItemIndex --
                            curItemIndex = max(0, curItemIndex)
                        }

                    }

                    println("$TAG to targetPage-->$curItemIndex")
                    val viewHolder = itemViewHolders[curItemIndex]
                    scroller.startScroll(scrollX, 0, viewHolder.viewLeft-scrollX, 0, 350)
                    ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                    return true
                }

                override fun onUp(e1: MotionEvent?, e2: MotionEvent?, deltaX: Float, deltaY: Float): Boolean {
                    if (e2 == null) return false
                    println("$TAG onUp -->$curItemIndex")
                    val needDeltaX = calculateTargetDeltaX(curItemIndex, scrollX)
                    println("$TAG onUp scrollX-->$scrollX")
                    println("$TAG onUp needDeltaX-->$needDeltaX")
                    scroller.startScroll(scrollX, 0, -needDeltaX, 0, 350)
                    ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                    return false
                }

            })

    private fun calculateTargetDeltaX(curItemIndex: Int, scrollX: Int): Int{
        if(curItemIndex >= childCount) return -1
        val viewHolder = itemViewHolders[curItemIndex]
        val width = viewHolder.viewRight - viewHolder.viewLeft
        val toDesDistance = viewHolder.viewRight - scrollX
        return if (abs(toDesDistance) < 0.5f * width) {//滑动了一大半了
            -toDesDistance
        }else {
            width-toDesDistance
        }
    }

    private fun getScrollRange(): Int {
        return if (childCount == 0) 0 else
            children.sumBy { it.measuredWidth } - measuredWidth
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX
            println("$TAG  computeScroll->${currX != scrollX}")
            if (currX != scrollX) {
                scrollTo(currX, scrollY)
            }
            ViewCompat.postInvalidateOnAnimation(this@ReaderPager)

        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return gestureDetector.onTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        var childLeft = paddingLeft
        val childTop = paddingTop
        if (childCount > 0) {
            children.forEachIndexed { index, child->
                child.layout(childLeft, childTop,
                    childLeft + child.measuredWidth, childTop + child.measuredHeight)
                if (index < itemViewHolders.size) {
                    itemViewHolders[index].apply {
                        viewLeft = childLeft
                        viewRight = childLeft + child.measuredWidth
                        position = index
                    }
                }

                childLeft += child.measuredWidth

            }
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        //设置默认宽高和父view相同
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec))

        //子view宽高考虑padding值
        val childWidth = measuredWidth - paddingLeft - paddingRight
        val childHeight = measuredHeight - paddingTop - paddingRight

        //创建child的measureSpec
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)

        children.forEachIndexed { index, child ->
            if (child.isGone.not()) {
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
    }

    fun setAdapter(adapter: ReaderPagerAdapter<ReaderPager.ViewHolder>) {
        itemViewHolders.clear()
        removeAllViews()
        for (i in 0..adapter.getItemCount()) {
            val viewHolder = adapter.createViewHolder(this, adapter.getItemType(i))
            itemViewHolders.add(viewHolder)
            addView(viewHolder.view)
            adapter.bindViewHolder(itemViewHolders[i], i, adapter.getItemType(i))
        }
        println("$TAG setAdapter itemViewHolders size-->${itemViewHolders.size}")
        requestLayout()
    }

    open class ViewHolder constructor(val view: View) {
        var position = NO_POSITION
        var type = DEFAULT_TYPE
        var viewLeft = view.left
        var viewRight = view.right
    }

    companion object {
        const val TAG = "ReaderPager"

        const val DEFAULT_TYPE = -1
        const val NO_POSITION = -1
        private val interpolator = Interpolator { t ->
            var t = t
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }


}