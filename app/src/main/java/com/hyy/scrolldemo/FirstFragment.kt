package com.hyy.scrolldemo

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatTextView
import androidx.navigation.fragment.findNavController
import com.hyy.scrolldemo.databinding.FragmentFirstBinding
import com.hyy.scrolldemo.databinding.ItemPageBinding
import com.hyy.scrolldemo.databinding.ItemPageImageBinding
import kotlin.random.Random

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

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
                if (viewholder is TextViewHolder) {
                    viewholder.binding.tvTitle.apply {
                        "我是PAGE $position".also { text = it }
                        setBackgroundColor(Color.argb(Random.nextInt(255), Random.nextInt(255),Random.nextInt(255),Random.nextInt(255)))
                    }
                }else if (viewholder is ImageViewHolder) {
                    viewholder.binding.tvTitle.apply {
                        "我是PAGE $position".also { text = it }
                    }
                    viewholder.binding.root.setBackgroundColor(Color.argb(Random.nextInt(255), Random.nextInt(255),Random.nextInt(255),Random.nextInt(255)))

                }

            }

            override fun getItemType(position: Int): Int {
                return if (position%2==0) {
                    ITEM_TYPE_TEXT
                } else ITEM_TYPE_IMAGE
            }
        })
    }

    internal class ImageViewHolder(val binding: ItemPageImageBinding) : ReaderPager.ViewHolder(binding.root)

    internal class TextViewHolder(val binding: ItemPageBinding) : ReaderPager.ViewHolder(binding.root)
}