package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReaderPagerSinglePoint constructor(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    private var downScrollX: Int = 0
    private var preloadingPage: Boolean = false
    private lateinit var adapter: ReaderPagerAdapter<ReaderPager.ViewHolder>
    private val scroller: OverScroller = OverScroller(context, interpolator)

    private var curItemIndex = 0

    private val itemViewHolders = mutableListOf<ReaderPager.ViewHolder>()

    private val cacheItemViewHolders = mutableListOf<ReaderPager.ViewHolder>()

    private var pageScrollState = PAGE_STATE_IDLE
    private var pageDirection = PAGE_DIRECTION_CURRENT

    init {
        setWillNotDraw(false)
    }

    //默认GestureDetectorCompat不支持单纯的action_up事件
    //解决方案copy源码并让onFling不限制速度就可以了拿到up事件了
    private val gestureDetector = GestureDetectorCompat(
            context,
            object : GestureDetectorCompat.CustomOnGestureListener {
                override fun onDown(e: MotionEvent): Boolean {
                    println("$TAG onDown-->e--->${e.x}")
                    if (getPageState() == PAGE_STATE_FLING) {
                        scroller.abortAnimation()
                        ViewCompat.postInvalidateOnAnimation(this@ReaderPagerSinglePoint)
                    } else {
                        setPageState(PAGE_STATE_IDLE)
                        setPageDirection(PAGE_DIRECTION_CURRENT)
                    }
                    downScrollX = scrollX

                    printViewHolders()
                    return true
                }

                override fun onLongClicked(e: MotionEvent): Boolean {
                    //需要根据情况返回true或者false 如果返回true 则代表后续其他事件都没用了
                    return false
                }

                override fun onShowPress(e: MotionEvent) {
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (getPageState() == PAGE_STATE_FLING) {
                        scroller.abortAnimation()
                        ViewCompat.postInvalidateOnAnimation(this@ReaderPagerSinglePoint)
                    } else {
                        //判断点击区域 然后看是弹出菜单还是。。

                    }
                    return true
                }

                override fun onScroll(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                ): Boolean {
                    requestDisallowInterceptTouchEvent(true)
                    val deltaX = e1.x - e2.x
                    val canScroll = ((downScrollX + deltaX < 0) ||
                            (downScrollX + deltaX > getScrollRange())).not()

                    if (getPageDirection() == PAGE_DIRECTION_CURRENT) {
                        if (deltaX > 0) {
                            preloadNextPage(curItemIndex + 1)
                        } else {
                            preloadPrePage(curItemIndex - 1)
                        }
                    }

                    println("$TAG onScroll deltaX-->$deltaX")
                    if (canScroll) {
                        if (deltaX > 0) {
                            setPageDirection(PAGE_DIRECTION_NEXT)
                        } else {
                            setPageDirection(PAGE_DIRECTION_PRE)
                        }
                        scrollTo((downScrollX + deltaX).toInt(), 0)
                        setPageState(PAGE_STATE_SCROLL)
                    } else {
                        //需要还有数据了的时候拓展
                    }

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
                        println("$TAG Fling to next page need ++  $curItemIndex")
                        itemHolder?.run {
                            if (scrollX < this.viewLeft) {
                                //这种情况是前面是往右滑动 最后抬起手指fling的时候 方向是向左的
                                //这个时候curItemIndex不需要变
                            } else {
                                preloadNextPage(curItemIndex + 1)
                                curItemIndex++
                                curItemIndex = min(itemViewHolders[itemViewHolders.size - 1].position, curItemIndex)
                                checkNeedRemoveSomeUselessItemViewHolder()
                            }
                        }

                    } else {
                        println("$TAG Fling to pre page need--")
                        itemHolder?.run {
                            if (scrollX > this.viewLeft) {
                                //这种情况是前面是往左滑动 最后抬起手指fling的时候 方向是向右的
                                //这个时候curItemIndex不需要变
                            } else {
                                preloadPrePage(curItemIndex - 1)
                                curItemIndex--
                                curItemIndex = max(0, curItemIndex)
                                checkNeedRemoveSomeUselessItemViewHolder()
                            }
                        }

                    }

                    itemViewHolders.find { it.position == curItemIndex }?.run {
                        scroller.startScroll(scrollX, 0, this.viewLeft - scrollX, 0, 350)
                        setPageState(PAGE_STATE_FLING)
                        ViewCompat.postInvalidateOnAnimation(this@ReaderPagerSinglePoint)
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
                    setPageState(PAGE_STATE_FLING)
                    ViewCompat.postInvalidateOnAnimation(this@ReaderPagerSinglePoint)
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
        if (preloadingPage.not() && curPreIndex > 0) {
            preloadingPage = true
            val preIndex = curPreIndex - 1
            checkNeedCreatePage(preIndex, true)
            preloadingPage = false
        }
    }

    private fun checkNeedCreatePage(index: Int, direction: Boolean) {
        if (itemViewHolders.isEmpty()) return
        //先判断是否已经存在于itemViewHolders中了
        if (direction) {
            val viewHolder = itemViewHolders[0]
            if (viewHolder.position == index && viewHolder.type == adapter.getItemType(index)) return
        } else {
            val viewHolder = itemViewHolders[itemViewHolders.size - 1]
            if (viewHolder.position == index && viewHolder.type == adapter.getItemType(index)) return
        }

        val cacheItemViewHolder = cacheItemViewHolders.find { it.type == adapter.getItemType(index) }
        if (cacheItemViewHolder != null) {
            cacheItemViewHolders.remove(cacheItemViewHolder)
            //reassign viewholder properties
            cacheItemViewHolder.position = index
            if (direction) {
                itemViewHolders.add(0, cacheItemViewHolder)
                // addView(cacheItemViewHolder.view, 0)
            } else {
                itemViewHolders.add(cacheItemViewHolder)
                //addView(cacheItemViewHolder.view)
            }
            adapter.bindViewHolder(cacheItemViewHolder, index, adapter.getItemType(index))
            requestLayout()

        } else {
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
        }

    }

    private fun preloadNextPage(curNextIndex: Int) {
        if (preloadingPage.not() && curNextIndex < adapter.getItemCount() - 1) {
            preloadingPage = true
            val nextIndex = curNextIndex + 1
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

    private fun calculateTargetDeltaX(curItemIndex: Int, scrollX: Int): Int {
        val viewHolder = itemViewHolders.find { it.position == curItemIndex }
        viewHolder?.run {
            //往后面翻页和往前翻页计算不一样
            val toNextPage = getPageDirection() == PAGE_DIRECTION_NEXT
            if (toNextPage) {
                val width = viewHolder.viewRight - viewHolder.viewLeft
                val toDesDistance = viewHolder.viewRight - scrollX
                return if (abs(toDesDistance) < 0.5f * width) {//滑动了一大半了 需要滑动到下一页
                    this@ReaderPagerSinglePoint.curItemIndex++
                    -toDesDistance
                } else {//没滑动一半宽度 需要回滚
                    width - toDesDistance
                }
            } else {
                val width = viewHolder.viewRight - viewHolder.viewLeft
                val pageOffset = viewHolder.viewLeft - scrollX
                return if (abs(pageOffset) < 0.5f * width) {//没滑动一半宽度 需要回滚
                    -pageOffset
                } else {//滑动了一大半了 需要滑动到下一页
                    this@ReaderPagerSinglePoint.curItemIndex--
                    width - pageOffset
                }
            }

        }

        return -1

    }

    private fun getScrollRange(): Int {
        return if (itemViewHolders.size == 0) 0 else
            itemViewHolders[itemViewHolders.size - 1].viewLeft
    }

    override fun computeScroll() {
        if (getPageState() == PAGE_STATE_FLING) {
            if (scroller.computeScrollOffset()) {
                val currX = scroller.currX
                println("$TAG  computeScroll->${currX != scrollX}")
                if (currX != scrollX) {
                    scrollTo(currX, scrollY)
                }
                ViewCompat.postInvalidateOnAnimation(this@ReaderPagerSinglePoint)

            } else {
                scrollTo(scroller.currX, scrollY)
                checkNeedRemoveSomeUselessItemViewHolder()
                setPageState(PAGE_STATE_IDLE)
                setPageDirection(PAGE_DIRECTION_CURRENT)
            }
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return gestureDetector.onTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        var childLeft = paddingLeft
        val childTop = paddingTop
        if (childCount > 0) {
            itemViewHolders.forEachIndexed { index, viewHolder ->
                viewHolder.view.let { child ->
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