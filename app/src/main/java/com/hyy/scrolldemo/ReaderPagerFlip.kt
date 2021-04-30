package com.hyy.scrolldemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.OverScroller
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReaderPagerFlip constructor(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    private var preloadingPage: Boolean = false
    private lateinit var adapter: ReaderPagerAdapter<ReaderPager.ViewHolder>
    private val scroller: OverScroller = OverScroller(context, interpolator)


    private var curItemIndex = 0

    private val itemViewHolders = mutableListOf<ReaderPager.ViewHolder>()

    private val cacheItemViewHolders = mutableListOf<ReaderPager.ViewHolder>()

    private var pageScrollState = PAGE_STATE_IDLE
    private var pageDirection = PAGE_DIRECTION_CURRENT

    private var curView: View? = null

    //定义最大最小阈值 在设置adapter或者更新数据的时候会重置
    private var minIndex: Int = -1
    private var maxIndex: Int = -1

    private var pageHeight = 0
    private var pageWidth = 0

    private val menuAreaRect = RectF()

    init {
        setWillNotDraw(false)
    }

    //默认GestureDetectorCompat不支持单纯的action_up事件
    //解决方案copy源码并让onFling不限制速度就可以了拿到up事件了
    private val gestureDetector = GestureDetectorCompat(
            context,
            object : GestureDetectorCompat.CustomOnGestureListener {
                override fun onDown(e: MotionEvent): Boolean {
                    println("$TAG onDown currIndex-->$curItemIndex")
                    if (getPageState() == PAGE_STATE_FLING) {
                        scroller.abortAnimation()
                        ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
                    } else {
                        setPageState(PAGE_STATE_IDLE)
                        setPageDirection(PAGE_DIRECTION_CURRENT)
                    }
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
                    println("$TAG onSingleTapUp-->")
                    if (getPageState() == PAGE_STATE_FLING) {
                        scroller.abortAnimation()
                        ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
                    } else {
                        //判断点击区域 然后看是弹出菜单还是。。
                        if (menuAreaRect.contains(e.x, e.y)) {
                            Toast.makeText(context, "弹出菜单区域", Toast.LENGTH_SHORT).show()
                        } else if (isInNextPageArea(e.x, e.y)) {
                            if (curItemIndex == maxIndex) {
                                Toast.makeText(context, "已经是最后一页了 需要往后加载更多数据", Toast.LENGTH_SHORT).show()
                            } else {
                                curView = itemViewHolders.find { it.position == curItemIndex }?.view
                                animateToNextPage()
                                setPageDirection(PAGE_DIRECTION_NEXT)
                                setPageState(PAGE_STATE_FLING)
                            }

                        } else if (isInPrePageArea(e.x, e.y)) {
                            if (curItemIndex == minIndex) {
                                Toast.makeText(context, "已经是首页了 往前加载更多数据", Toast.LENGTH_SHORT).show()
                            } else {
                                curView = itemViewHolders.find { it.position == curItemIndex-1 }?.view
                                animateToPrePage()
                                setPageDirection(PAGE_DIRECTION_PRE)
                                setPageState(PAGE_STATE_FLING)
                            }
                        }
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
                    val canScrollToNext = curItemIndex < maxIndex && deltaX > 0
                    val canScrollPre = curItemIndex > 0 && deltaX < 0

                    if (getPageDirection() == PAGE_DIRECTION_CURRENT) {
                        if (deltaX > 0) {
                            preloadNextPage(curItemIndex + 1)
                        } else {
                            preloadPrePage(curItemIndex - 1)
                        }
                    }

                    if (canScrollToNext || canScrollPre) {
                        if (deltaX > 0) {//翻到下一页
                            if (isToPrePage()) {//禁止一次性滑动能翻三页的逻辑
                                curView?.translationX = -measuredWidth.toFloat()
                            } else {
                                if (curView == null || getPageDirection() != PAGE_DIRECTION_NEXT) {
                                    curView = itemViewHolders.find { it.position == curItemIndex }?.view
                                    println("$TAG curView--> ${curView?.tag}")
                                }
                                setPageDirection(PAGE_DIRECTION_NEXT)
                                Log.d(TAG, "onScroll: ${curView?.translationX}")
                                curView?.translationX = -deltaX
                            }

                        } else {//翻到上一页
                            if (isToNextPage()) {
                                curView?.translationX = 0f
                            } else {
                                if (curView == null || getPageDirection() != PAGE_DIRECTION_PRE) {
                                    curView = itemViewHolders.find { it.position == curItemIndex - 1 }?.view
                                }
                                setPageDirection(PAGE_DIRECTION_PRE)
                                curView?.translationX = -measuredWidth - deltaX
                            }
                        }
                        setPageState(PAGE_STATE_SCROLL)
                    } else {
                        if (canScrollToNext.not()) {
                            println("$TAG LoadMoreList to next")
                            //询问是否还有后面的章节
                        } else if (canScrollPre.not()) {
                            println("$TAG LoadMoreList to pre")
                            //询问是否还有前面的章节
                        }
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

                    curView?.run {

                        if (velocityX < 0f) {//fling to next Page
                            if (isToNextPage()) {//此时才是真正的往后翻页
                                scrollNextPageIntoScreen()
                            } else {//回滚到原来的位置
                                rollbackCurPage(translationX.toInt(), -measuredWidth)
                            }
                            println("$TAG  onFling startX-->${translationX.toInt()}")
                            println("$TAG  onFling measureWidth-->${measuredWidth}")
                            println("$TAG  onFling deltaX-->${(-(measuredWidth + translationX)).toInt()}")

                        } else {
                            if (isToPrePage()) {//此时才是真正的往前翻页
                                scrollPrePageOutScreen()
                            } else {//回滚到原来的位置
                                rollbackCurPage(translationX.toInt(), 0)
                            }
                            println("$TAG  onFling startX-->${translationX.toInt()}")
                        }

                        setPageState(PAGE_STATE_FLING)
                    }
//
                    return true
                }

                override fun onUp(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        deltaX: Float,
                        deltaY: Float
                ): Boolean {
                    println("$TAG onUp curView-->${curView}")
                    curView?.run {
                        if (isToNextPage()) {
                            if (abs(translationX) / measuredWidth < 0.5) {//回滚
                                rollbackCurPage(translationX.toInt(), 0)
                            } else {//滚到下一页 并更改curIndex
                                scrollNextPageIntoScreen()
                            }
                        } else {
                            if (abs(translationX) / measuredWidth < 0.5) {//将上一页滚回屏幕 并更改curIndex
                                scrollPrePageOutScreen()
                            } else {//回滚
                                rollbackCurPage(translationX.toInt(), -measuredWidth)
                            }
                        }
                        setPageState(PAGE_STATE_FLING)
                    }
                    return true
                }

            })

    private fun animateToNextPage() {
        curView?.run {
            preloadNextPage(curItemIndex + 1)
            curItemIndex++
            curItemIndex = min(itemViewHolders[0].position, curItemIndex)
            scroller.startScroll(0, 0, -pageWidth, 0, 350)
            ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
        }
    }

    private fun animateToPrePage() {
        curView?.run {
            preloadPrePage(curItemIndex - 1)
            curItemIndex--
            curItemIndex = max(0, curItemIndex)
            scroller.startScroll(-pageWidth, 0, pageWidth, 0, 350)
            ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
        }

    }

    private fun scrollNextPageIntoScreen() {
        curView?.run {
            preloadNextPage(curItemIndex + 1)
            curItemIndex++
            curItemIndex = min(itemViewHolders[0].position, curItemIndex)
            checkNeedRemoveSomeUselessItemViewHolder()
            scroller.startScroll((translationX).toInt(), 0, -measuredWidth - translationX.toInt(), 0, 350)
            ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
        }
    }

    private fun scrollPrePageOutScreen() {
        curView?.run {
            preloadPrePage(curItemIndex - 1)
            curItemIndex--
            curItemIndex = max(0, curItemIndex)
            checkNeedRemoveSomeUselessItemViewHolder()
            scroller.startScroll(translationX.toInt(), 0, -translationX.toInt(), 0, 350)
            ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)
        }

    }

    private fun rollbackCurPage(curTranslation: Int, targetTranslation: Int) {
        scroller.startScroll(curTranslation, 0, targetTranslation - curTranslation, 0, 350)
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun isToNextPage(): Boolean = getPageDirection() == PAGE_DIRECTION_NEXT

    private fun isToPrePage(): Boolean = getPageDirection() == PAGE_DIRECTION_PRE

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
                removeView(it.view)
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
        val viewHolder = itemViewHolders.find { it.position == index && it.type == adapter.getItemType(index) }
        if (viewHolder != null) return

        val cacheItemViewHolder = cacheItemViewHolders.find { it.type == adapter.getItemType(index) }
        if (cacheItemViewHolder != null) {
            cacheItemViewHolders.remove(cacheItemViewHolder)
            //reassign viewholder properties
            cacheItemViewHolder.position = index
            if (direction) {//pre
                itemViewHolders.add(cacheItemViewHolder)
                cacheItemViewHolder.view.translationX = -measuredWidth * 1f
                addView(cacheItemViewHolder.view)
            } else {//next
                println("$TAG addItemViewHolder-->$index")
                itemViewHolders.add(0, cacheItemViewHolder)
                cacheItemViewHolder.view.translationX = 0f
                addView(cacheItemViewHolder.view, 0)
            }
            adapter.bindViewHolder(cacheItemViewHolder, index, adapter.getItemType(index))
            requestLayout()

        } else {
            val viewHolder = adapter.createViewHolder(this, adapter.getItemType(index)).apply {
                type = adapter.getItemType(index)
                position = index
            }
            if (direction) {
                itemViewHolders.add(viewHolder)
                viewHolder.view.translationX = -measuredWidth * 1f
                addView(viewHolder.view)
            } else {
                itemViewHolders.add(0, viewHolder)
                viewHolder.view.translationX = 0f
                addView(viewHolder.view, 0)
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

    override fun computeScroll() {
        if (getPageState() == PAGE_STATE_FLING) {
            if (scroller.computeScrollOffset()) {
                val currX = scroller.currX
                println("$TAG  computeScroll->${currX}")
                if (currX != curView?.translationX ?: 0) {
//                    scrollTo(currX, scrollY)
                    curView?.translationX = currX.toFloat()
                }
                ViewCompat.postInvalidateOnAnimation(this@ReaderPagerFlip)

            } else {
                curView?.translationX = scroller.currX.toFloat()
                checkNeedRemoveSomeUselessItemViewHolder()
                curView = null
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
                println("")
                viewHolder.view.let { child ->
                    child.layout(0, childTop,
                            measuredWidth, childTop + child.measuredHeight)
                    viewHolder.apply {
                        viewLeft = childLeft
                        viewRight = childLeft + child.measuredWidth
                        position = viewHolder.position
                    }
                }
            }
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        //设置默认宽高和父view相同
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec))

        pageWidth = measuredWidth
        pageHeight = measuredHeight
        //设置menu点击区域
        menuAreaRect.set(pageWidth * START_THRESHOLD, pageHeight * START_THRESHOLD, pageWidth * END_THRESHOLD, pageHeight * END_THRESHOLD)
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

    private fun isInPrePageArea(x: Float, y: Float): Boolean {
        return (x <= pageWidth * START_THRESHOLD) || (x <= pageWidth * END_THRESHOLD && y < pageHeight * START_THRESHOLD)
    }

    private fun isInNextPageArea(x: Float, y: Float): Boolean {
        return (x >= pageWidth * END_THRESHOLD) || (x >= pageWidth * START_THRESHOLD && y >= pageHeight * END_THRESHOLD)

    }

    fun setAdapter(adapter: ReaderPagerAdapter<ReaderPager.ViewHolder>) {
        itemViewHolders.clear()
        removeAllViews()
        this.adapter = adapter
        if (adapter.getItemCount() == 0) return

        //在叠加的模式下  addView应该通过
        //默认最左边为minIndex
        //最右边为maxIndex
        //预加载上一章节会导致minIndex变化
        //预加载下一章节会导致maxIndex变化
        minIndex = 0
        maxIndex = adapter.getItemCount() - 1
        for (i in 0 until 2) {
            val viewHolder = adapter.createViewHolder(this, adapter.getItemType(i)).apply {
                position = i
                type = adapter.getItemType(i)
            }
            itemViewHolders.add(0, viewHolder)
            addView(viewHolder.view, 0)
            adapter.bindViewHolder(itemViewHolders[0], i, adapter.getItemType(i))
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

        //点击区域边界阈值
        private const val START_THRESHOLD = 0.25f
        private const val END_THRESHOLD = 0.75f

        private val interpolator = Interpolator { t ->
            var t = t
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }


}