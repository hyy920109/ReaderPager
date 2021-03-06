package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.OverScroller
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReaderPager constructor(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    private var preloadingPage: Boolean = false
    private lateinit var adapter: ReaderPagerAdapter<ViewHolder>
    private val scroller: OverScroller = OverScroller(context, interpolator)

    private var curItemIndex = 0

    private val itemViewHolders = mutableListOf<ViewHolder>()

    private val cacheItemViewHolders = mutableListOf<ViewHolder>()

    private var totalScrollX = 0.0f
    private var offsetPage = 0.0f

    private var pageScrollState = PAGE_STATE_IDLE
    private var pageDirection = PAGE_DIRECTION_CURRENT


    init {

    }

    //默认GestureDetectorCompat不支持单纯的action_up事件
    //解决方案copy源码并让onFling不限制速度就可以了拿到up事件了
    private val gestureDetector= GestureDetectorCompat(
        context,
        object : GestureDetectorCompat.CustomOnGestureListener {
            override fun onDown(e: MotionEvent?): Boolean {
                println("$TAG onDown-->e--->${e?.x}")
                totalScrollX = 0.0f
                offsetPage = 0f
                scroller.forceFinished(true)
                printViewHolders()
                return true
            }

            override fun onLongClicked(e: MotionEvent?): Boolean {
                //需要根据情况返回true或者false 如果返回true 则代表后续其他事件都没用了

                return false
            }

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                //纯粹的点击事件
                if (scrollX % measuredWidth == 0) return true
                val needDeltaX = calculateTargetDeltaX(curItemIndex, scrollX)
                scroller.startScroll(scrollX, 0, -needDeltaX, 0, 350)
                ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                checkNeedRemoveSomeUselessItemViewHolder()
                return true
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                requestDisallowInterceptTouchEvent(true)
                val canScroll = ((scrollX + distanceX < 0) ||
                        (scrollX + distanceX > getScrollRange())).not()

                if (canScroll) {
                    //记录总共滑动距离
                    totalScrollX += distanceX
                    val totalOffset = totalScrollX / measuredWidth

                    if (totalOffset - offsetPage > 0) {
                        preloadNextPage(curItemIndex + 1)
                        //处理多指滑动问题 滚动超过一屏幕
                        if (totalOffset - offsetPage >= 1f) {
                            curItemIndex++
                            offsetPage = totalOffset
                            checkNeedRemoveSomeUselessItemViewHolder()
                            Toast.makeText(context, "往前划到第$curItemIndex 页数据了", Toast.LENGTH_SHORT).show()
                        }
                        setPageDirection(PAGE_DIRECTION_NEXT)
                    } else  {
                        preloadPrePage(curItemIndex - 1)
                        if (totalOffset - offsetPage <= -1f) {
                            offsetPage = totalOffset
                            curItemIndex--
                            checkNeedRemoveSomeUselessItemViewHolder()
                            Toast.makeText(context, "后翻划到第$curItemIndex 页数据了", Toast.LENGTH_SHORT).show()
                        }
                        setPageDirection(PAGE_DIRECTION_PRE)
                    }
                    scrollBy((distanceX).toInt(), 0)

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
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val itemHolder = itemViewHolders.find { it.position == curItemIndex }
                println("$TAG onFling-->$itemHolder")
                if (velocityX < 0f) {
                    println("$TAG Fling to left page need ++  $curItemIndex")
                    itemHolder?.run {
                        if (scrollX < this.viewLeft) {
                            //这种情况是前面是往右滑动 最后抬起手指fling的时候 方向是向左的
                            //这个时候curItemIndex不需要变
                        } else {
                            curItemIndex++
                            curItemIndex = min(itemViewHolders[itemViewHolders.size-1].position, curItemIndex)
                            checkNeedRemoveSomeUselessItemViewHolder()
                        }
                    }

                } else {
                    println("$TAG Fling to right page need-- $curItemIndex")
                    itemHolder?.run {
                        if (scrollX > this.viewLeft) {
                            //这种情况是前面是往左滑动 最后抬起手指fling的时候 方向是向右的
                            //这个时候curItemIndex不需要变
                        } else {
                            curItemIndex--
                            curItemIndex = max(0, curItemIndex)
                            checkNeedRemoveSomeUselessItemViewHolder()
                        }
                    }

                }

                itemViewHolders.find { it.position == curItemIndex }?.run {
                    scroller.startScroll(scrollX, 0, this.viewLeft - scrollX, 0, 350)
                    ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                }

                return true
            }

            override fun onUp(
                e1: MotionEvent,
                e2: MotionEvent,
                deltaX: Float,
                deltaY: Float
            ): Boolean {

                val needDeltaX = calculateTargetDeltaX(curItemIndex, scrollX)
                printViewHolders()
                scroller.startScroll(scrollX, 0, -needDeltaX, 0, 350)
                ViewCompat.postInvalidateOnAnimation(this@ReaderPager)
                return false
            }

        })

    private fun printViewHolders() {
        itemViewHolders.forEach {
            println("$TAG viewHolder-->${it.position}   left-->${it.viewLeft}")
        }
    }

    private fun checkNeedRemoveSomeUselessItemViewHolder() {
        println("$TAG checkNeedRemoveSomeUselessItemViewHolder-->$curItemIndex")
        itemViewHolders.filter { abs(it.position - curItemIndex) > 1 }.run {
            this.forEach {
                println("$TAG remove position-->${it.position}")
                itemViewHolders.remove(it)
                cacheItemViewHolders.add(it)
            }
        }
    }

    private fun preloadPrePage(curPreIndex: Int) {
        println("$TAG preloadNextPage-->nextIndex")
        if (preloadingPage.not() && curPreIndex > 0) {
            preloadingPage = true
            val preIndex = curPreIndex-1
            checkNeedCreatePage(preIndex, true)
            preloadingPage = false
        }
    }

    private fun checkNeedCreatePage(index: Int, direction: Boolean) {
        if (itemViewHolders.isEmpty())return
        //先判断是否已经存在于itemViewHolders中了
        if (direction) {
            val viewHolder = itemViewHolders[0]
            if (viewHolder.position == index && viewHolder.type == adapter.getItemType(index))return
        }else {
            val viewHolder = itemViewHolders[itemViewHolders.size-1]
            if (viewHolder.position == index && viewHolder.type == adapter.getItemType(index))return
        }

        val cacheItemViewHolder = cacheItemViewHolders.find { it.type == adapter.getItemType(index) }
        if (cacheItemViewHolder != null) {
            cacheItemViewHolders.remove(cacheItemViewHolder)
            //reassign viewholder properties
            cacheItemViewHolder.position = index
            adapter.bindViewHolder(cacheItemViewHolder, index, adapter.getItemType(index))
            if (direction) {
                itemViewHolders.add(0, cacheItemViewHolder)
               // addView(cacheItemViewHolder.view, 0)
            } else {
                itemViewHolders.add(cacheItemViewHolder)
                //addView(cacheItemViewHolder.view)
            }
            postInvalidate()
            requestLayout()

        }else {
            val viewHolder = adapter.createViewHolder(this, adapter.getItemType(index)).apply {
                type = adapter.getItemType(index)
                position = index
            }
            if (direction) {
                itemViewHolders.add(0, viewHolder)
                addView(viewHolder.view, 0)
            } else {
                itemViewHolders.add(viewHolder)
                addView(viewHolder.view)
            }
            adapter.bindViewHolder(viewHolder, index, adapter.getItemType(index))
//                requestLayout()
        }

    }

    private fun preloadNextPage(curNextIndex: Int) {
        println("$TAG preloadNextPage-->$curNextIndex")
        if (preloadingPage.not() && curNextIndex < adapter.getItemCount()-1) {
            preloadingPage = true
            val nextIndex = curNextIndex+1
            checkNeedCreatePage(nextIndex, false)
            preloadingPage = false
        }
    }

    private fun setPageState(pageState: Int) {
        pageScrollState = PAGE_STATE_MASK and pageState
    }

    private fun getPageState(): Int = PAGE_STATE_MASK and pageScrollState


    private fun setPageDirection(pageDir: Int) {
        pageDirection = PAGE_DIRECTION_MASK and pageDir
    }

    private fun getPageDirection(): Int = PAGE_DIRECTION_MASK and pageDirection

    private fun calculateTargetDeltaX(curItemIndex: Int, scrollX: Int): Int{
//        if(curItemIndex >= childCount) return -1
        val viewHolder = itemViewHolders.find { it.position == curItemIndex }
        viewHolder?.run {
            //往后面翻页和往前翻页计算不一样
            val toNextPage = getPageDirection() == PAGE_DIRECTION_NEXT
            if (toNextPage) {
                val width = viewHolder.viewRight - viewHolder.viewLeft
                val toDesDistance = viewHolder.viewRight - scrollX
                return if (abs(toDesDistance) < 0.5f * width) {//滑动了一大半了 需要滑动到下一页
                    this@ReaderPager.curItemIndex ++
                    -toDesDistance
                }else {//没滑动一半宽度 需要回滚
                    width-toDesDistance
                }
            }else {
                val width = viewHolder.viewRight - viewHolder.viewLeft
                val pageOffset = viewHolder.viewLeft - scrollX
                return if (abs(pageOffset) < 0.5f * width) {//没滑动一半宽度 需要回滚
                    -pageOffset
                }else {//滑动了一大半了 需要滑动到下一页
                    this@ReaderPager.curItemIndex --
                    width-pageOffset
                }
            }

        }

        return -1

    }

    private fun getScrollRange(): Int {
        return if (itemViewHolders.size == 0) 0 else
            itemViewHolders[itemViewHolders.size-1].viewLeft
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX
            println("$TAG  computeScroll->${currX != scrollX}")
            if (currX != scrollX) {
                scrollTo(currX, scrollY)
            }
            ViewCompat.postInvalidateOnAnimation(this@ReaderPager)

        }else {
            if(scrollX % measuredWidth == 0) {
                println("$TAG  computeScroll idle state-->$curItemIndex")
                checkNeedRemoveSomeUselessItemViewHolder()
            }
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
            itemViewHolders.forEachIndexed { index, viewHolder ->
                viewHolder.view.let { child->
                    childLeft = viewHolder.position * measuredWidth
                    child.layout(childLeft, childTop,
                        childLeft + child.measuredWidth, childTop + child.measuredHeight)
                    viewHolder.apply {
                        viewLeft = childLeft
                        viewRight = childLeft + child.measuredWidth
                        position = viewHolder.position
                    }

//                    childLeft += child.measuredWidth
                }

            }
//            children.forEachIndexed { index, child->
//                child.layout(childLeft, childTop,
//                    childLeft + child.measuredWidth, childTop + child.measuredHeight)
//                if (index < itemViewHolders.size) {
//                    itemViewHolders[index].apply {
//                        viewLeft = childLeft
//                        viewRight = childLeft + child.measuredWidth
//                        position = index
//                    }
//                }
//
//                childLeft += child.measuredWidth
//
//            }
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
        this.adapter = adapter
        if (adapter.getItemCount() == 0) return

        for (i in 0 until 2) {
            val viewHolder = adapter.createViewHolder(this, adapter.getItemType(i)).apply {
                position = i
                type = adapter.getItemType(i)
            }
            itemViewHolders.add(viewHolder)
            addView(viewHolder.view)
            adapter.bindViewHolder(itemViewHolders[i], i, adapter.getItemType(i))
        }
        println("$TAG setAdapter itemViewHolders size-->${itemViewHolders.size}")
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        itemViewHolders.clear()
        cacheItemViewHolders.clear()
        removeAllViews()
    }

    open class ViewHolder constructor(val view: View) {
        var position = NO_POSITION
        var type = DEFAULT_TYPE
        var viewLeft = view.left
        var viewRight = view.right
    }

    companion object {
        const val TAG = "ReaderPager"

        //define page state
        private const val PAGE_STATE_IDLE = 0x00000100
        private const val PAGE_STATE_SCROLL = 0x00000200
        private const val PAGE_STATE_FLING = 0x00000400
        private const val PAGE_STATE_MASK = 0x00000700

        //define direction state
        private const val PAGE_DIRECTION_CURRENT = 0x00000100
        private const val PAGE_DIRECTION_NEXT = 0x00000200
        private const val PAGE_DIRECTION_PRE = 0x00000400
        private const val PAGE_DIRECTION_MASK = 0x00000700

        private const val DEFAULT_TYPE = -1
        private const val NO_POSITION = -1
        private val interpolator = Interpolator { t ->
            var t = t
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }


}