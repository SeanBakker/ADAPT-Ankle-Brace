package com.example.adaptanklebrace.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R

class TimerDialogFragment(
    private val isROMMetric: Boolean = false,
    private val isGaitMetric: Boolean = false,
    private val onTimerFinished: () -> Unit
) : DialogFragment() {

    private lateinit var timerText: TextView
    private var totalTestDurationMillis: Long = 5000 // Default to 5s (ROM test)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_timer_dialog, container, false)
        timerText = view.findViewById(R.id.timerText)
        view.visibility = View.INVISIBLE
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(300, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

            if (isROMMetric) {
                params.y = 200
                totalTestDurationMillis = 5000 // 5 seconds for ROM test
            } else if (isGaitMetric) {
                params.y = 350
                totalTestDurationMillis = 10000 // 10 seconds for Gait test
            }
            attributes = params
        }

        view?.postDelayed({
            view?.visibility = View.VISIBLE
        }, 200)

        startTimer()
    }

    @SuppressLint("DefaultLocale, SetTextI18n")
    private fun startTimer() {
        object : CountDownTimer(totalTestDurationMillis, 10) { // Update every 10ms
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val milliseconds = (millisUntilFinished % 1000) / 10  // Convert to two-digit format

                timerText.text = String.format("%02d:%02d", seconds, milliseconds)
            }

            override fun onFinish() {
                timerText.text = "Done!"
                view?.postDelayed({
                    dismiss() // Close after 1 second
                    onTimerFinished()
                }, 1000)
            }
        }.start()
    }
}
