package com.hyy.scrolldemo

import android.view.ViewGroup

abstract class ReaderPagerAdapter<in T: ReaderPager.ViewHolder> {

    companion object {
        const val ITEM_TYPE_DEFAULT = -1
    }

    abstract fun getItemCount(): Int

    abstract fun createViewHolder(parent: ViewGroup, itemType: Int): ReaderPager.ViewHolder

    abstract fun bindViewHolder(viewholder: T, position: Int, itemType: Int)

    open fun getItemType(position: Int): Int = ITEM_TYPE_DEFAULT
}