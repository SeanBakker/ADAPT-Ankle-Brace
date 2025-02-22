package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric

class StartWarningFragment(
    private val context: Context,
    private val exercise: Exercise? = null,
    private val metric: Metric? = null
) : DialogFragment() {

    // Define an interface to communicate with the activity
    interface OnStartExerciseOrMetricListener {
        fun onStartExerciseActivity(context: Context, exercise: Exercise)
        fun onStartMetricActivity(context: Context, metric: Metric)
    }

    private var listener: OnStartExerciseOrMetricListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start_warning, container, false)

        val startButton: Button = view.findViewById(R.id.startBtn)
        val titleWarning: TextView = view.findViewById(R.id.titleWarning)

        // Setup text views
        if (exercise != null) {
            titleWarning.text = getString(R.string.startExerciseWarning)
            startButton.text = getString(R.string.startExerciseBtn)
        } else if (metric != null) {
            titleWarning.text = getString(R.string.startMetricWarning)
            startButton.text = getString(R.string.startMetricBtn)
        }

        startButton.setOnClickListener {
            if (exercise != null) {
                startExercise()
            } else if (metric != null) {
                startMetric()
            }
        }

        return view
    }

    private fun startExercise() {
        // Call back to the activity through the interface
        listener?.onStartExerciseActivity(context, exercise!!)
        dismiss() // Close the dialog
    }

    private fun startMetric() {
        // Call back to the activity through the interface
        listener?.onStartMetricActivity(context, metric!!)
        dismiss() // Close the dialog
    }

    // Ensure the activity implements the interface
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnStartExerciseOrMetricListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnStartExerciseOrMetricListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null // Avoid memory leaks
    }
}
