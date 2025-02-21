package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R

class CountdownDialogFragment(
    private val isROMMetric: Boolean = false,
    private val isGaitMetric: Boolean = false,
    private val onCountdownFinished: () -> Unit
) : DialogFragment() {

    private lateinit var countdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensures the background outside the dialog is transparent
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_countdown_dialog, container, false)
        countdownText = view.findViewById(R.id.countdownText)
        view.visibility = View.INVISIBLE
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(150, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL // Move to bottom center
            // Adjust vertical offset
            if (isROMMetric) {
                params.y = 300
            } else if (isGaitMetric) {
                params.y = 450
            }
            attributes = params
        }

        view?.postDelayed({
            view?.visibility = View.VISIBLE
        }, 200)
        startCountdown()
    }

    private fun startCountdown() {
        // Configure 3s countdown + 1s of showing text: "Start!"
        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                countdownText.text = if (secondsRemaining > 0) {
                    secondsRemaining.toString()
                } else {
                    "Start!"
                }
            }

            override fun onFinish() {
                dismiss()
                onCountdownFinished()  // Notify when countdown ends
            }
        }.start()
    }
}
