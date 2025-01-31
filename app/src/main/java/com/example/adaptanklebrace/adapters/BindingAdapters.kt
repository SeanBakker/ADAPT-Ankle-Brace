package com.example.adaptanklebrace.adapters

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.adaptanklebrace.R

object BindingAdapters {

    @BindingAdapter("intToStringText")
    @JvmStatic
    fun setIntToString(view: TextView, value: Int?) {
        view.text = value?.toString() ?: ""
    }

    @BindingAdapter("imageFromResourceId")
    @JvmStatic
    fun setImageFromResourceId(view: ImageView, imageId: Int?) {
        imageId?.let {
            //todo: add proper exercise images
            val drawableResId = when (it) {
                1 -> R.drawable.plantarflexion_guide // exercise #1
                2 -> R.drawable.dorsiflexion_guide // exercise #2
                3 -> R.drawable.baseline_healing_24 // exercise #3
                4 -> R.drawable.baseline_healing_24 // exercise #4
                5 -> R.drawable.baseline_healing_24 // exercise #5
                6 -> R.drawable.baseline_healing_24 // exercise #6
                7 -> R.drawable.baseline_healing_24 // exercise #7
                else -> R.drawable.baseline_error_24 // error
            }
            view.setImageResource(drawableResId)
        }
    }
}
