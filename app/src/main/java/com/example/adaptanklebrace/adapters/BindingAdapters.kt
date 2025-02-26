package com.example.adaptanklebrace.adapters

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.VideoView
import androidx.databinding.BindingAdapter
import com.example.adaptanklebrace.R

object BindingAdapters {

    @BindingAdapter("intToStringText")
    @JvmStatic
    fun setIntToString(view: TextView, value: Int?) {
        view.text = value?.toString() ?: ""
    }

    @BindingAdapter("videoFromResourceId")
    @JvmStatic
    fun setVideoFromResourceId(view: VideoView, videoId: Int?) {
        videoId?.let {
            val videoResId = when (it) {
                1 -> R.raw.plantarflexion_video // plantar flexion
                2 -> R.raw.dorsiflexion_video // dorsiflexion
                3 -> R.raw.inversion_video // inversion
                4 -> R.raw.eversion_video // eversion
                5 -> R.raw.rom_video // rom metric
                6 -> R.raw.gait_video // gait metric
                else -> R.drawable.adapt_app_white_logo // other
            }
            videoResId.let { resId ->
                val videoUri = Uri.parse("android.resource://${view.context.packageName}/$resId")
                view.setVideoURI(videoUri)

                // Start video automatically & looping
                view.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start() // Auto-play video

                    mediaPlayer.setOnCompletionListener {
                        // Add delay before restarting
                        Handler(Looper.getMainLooper()).postDelayed({
                            view.start() // Restart video after delay
                        }, 1000) // 1000ms = 1 second delay
                    }
                }
            }
        }
    }
}
