package com.hyy.scrolldemo

import android.view.View
import android.view.ViewGroup

/**
 *Create by hyy on 2021/4/19
 */

fun Int.getMeasureMode(): String {
    return when(View.MeasureSpec.getMode(this)) {
        View.MeasureSpec.EXACTLY -> {
            "EXACTLY"
        }
        View.MeasureSpec.AT_MOST -> {
            "AT_MOST"
        }
        View.MeasureSpec.UNSPECIFIED ->{
            "UNSPECIFIED"
        }

        else -> "Unknown"
    }
}

fun Int.getMeasureSize(): String {
    return when(View.MeasureSpec.getSize(this)) {
        ViewGroup.LayoutParams.MATCH_PARENT -> "MATCH_PARENT"
        ViewGroup.LayoutParams.WRAP_CONTENT -> "WRAP_CONTENT"
        else -> "$this"
    }
}