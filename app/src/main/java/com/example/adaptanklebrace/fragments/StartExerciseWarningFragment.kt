package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric

class StartExerciseWarningFragment(
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
        val view = inflater.inflate(R.layout.fragment_start_exercise_warning, container, false)

        val startExerciseButton: Button = view.findViewById(R.id.startExerciseBtn)
        startExerciseButton.setOnClickListener {
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
