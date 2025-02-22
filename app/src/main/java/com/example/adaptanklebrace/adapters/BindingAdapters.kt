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
                1 -> R.drawable.plantarflexion_guide // plantar flexion
                2 -> R.drawable.dorsiflexion_guide // dorsiflexion
                3 -> R.drawable.inversion_guide // inversion
                4 -> R.drawable.eversion_guide // eversion
                5 -> R.drawable.baseline_healing_24 // rom metric
                6 -> R.drawable.baseline_healing_24 // gait metric
                else -> R.drawable.adapt_app_white_logo // other
            }
            view.setImageResource(drawableResId)
        }
    }
}
