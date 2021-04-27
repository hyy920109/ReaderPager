package com.hyy.scrolldemo

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hyy.scrolldemo.databinding.FragmentFirstBinding
import com.hyy.scrolldemo.databinding.ItemPageBinding
import com.hyy.scrolldemo.databinding.ItemPageImageBinding
import kotlin.random.Random

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val contents = arrayOf("啦啦啦啦啦啦啦啦啦啦啦啦啦啦啦啦", "hahahahahahhahahha",
    "呵呵呵呵呵呵呵呵呵呵", "lolololololololo", "enennenenennenen")
    private val colors = arrayOf(Color.LTGRAY, Color.WHITE, Color.CYAN, Color.GRAY)
    private lateinit var binding: FragmentFirstBinding

    companion object {
        const val ITEM_TYPE_TEXT = 0
        const val ITEM_TYPE_IMAGE = 1
    }
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.readerPager.setAdapter(object : ReaderPagerAdapter<ReaderPager.ViewHolder>() {
            override fun getItemCount(): Int {
                return 5
            }

            override fun createViewHolder(
                parent: ViewGroup,
                itemType: Int
            ): ReaderPager.ViewHolder {
                if (itemType == ITEM_TYPE_IMAGE) {
                    val context = parent.context
                    return ImageViewHolder(ItemPageImageBinding.inflate(LayoutInflater.from(context), parent, false))
                } else {
                    val context = parent.context
                    return TextViewHolder(ItemPageBinding.inflate(LayoutInflater.from(context), parent, false))
                }

            }

            override fun bindViewHolder(
                viewholder: ReaderPager.ViewHolder,
                position: Int,
                itemType: Int
            ) {
                println("ReaderPager------>bindViewHolder-->$position")
                if (viewholder is TextViewHolder) {
                    viewholder.binding.tvTitle.apply {
                        "当前是PAGE$position".also { text = it }
                        setBackgroundColor(colors[Random.nextInt(4)])
                    }
                }else if (viewholder is ImageViewHolder) {
                    viewholder.binding.tvTitle.apply {
                        contents[position].also { text = it }
                    }
//                    viewholder.binding.root.setBackgroundColor(Color.argb(Random.nextInt(255), Random.nextInt(255),Random.nextInt(255),Random.nextInt(255)))

                }

            }

            override fun getItemType(position: Int): Int {
//                return if (position%2==0) {
//                    ITEM_TYPE_TEXT
//                } else ITEM_TYPE_IMAGE
                return ITEM_TYPE_TEXT
            }
        })
    }

    internal class ImageViewHolder(val binding: ItemPageImageBinding) : ReaderPager.ViewHolder(binding.root)

    internal class TextViewHolder(val binding: ItemPageBinding) : ReaderPager.ViewHolder(binding.root)
}