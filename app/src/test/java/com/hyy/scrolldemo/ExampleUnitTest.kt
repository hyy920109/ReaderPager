package com.hyy.scrolldemo

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private val PAGE_STATE_IDLE = 0x00000100
    private val PAGE_STATE_SCROLL = 0x00000200
    private val PAGE_STATE_FLING = 0x00000400
    private val PAGE_STATE_MASK = 0x00000700

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    @Test
    fun testPageState() {
        println("PAGE_STATE_IDLE-->$PAGE_STATE_IDLE")
        println("PAGE_STATE_SCROLL-->$PAGE_STATE_SCROLL")
        println("PAGE_STATE_FLING-->$PAGE_STATE_FLING")
        println("PAGE_STATE_MASK-->$PAGE_STATE_MASK")

        val pageState = PAGE_STATE_MASK and PAGE_STATE_SCROLL
        println("pageState-->$pageState")
        println("getPageState--> ${pageState and PAGE_STATE_MASK}")
    }

}