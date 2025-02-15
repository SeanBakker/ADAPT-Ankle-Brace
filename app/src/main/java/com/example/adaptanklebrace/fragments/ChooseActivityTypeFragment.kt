package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R

class ChooseActivityTypeFragment : DialogFragment() {

    interface ChooseActivityTypeListener {
        fun onChooseActivityExercise()
        fun onChooseActivityMetric()
    }

    private var listener: ChooseActivityTypeListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_choose_activity_type, container, false)

        // References to fields
        val chooseExerciseButton: Button = view.findViewById(R.id.chooseExerciseBtn)
        val chooseMetricButton: Button = view.findViewById(R.id.chooseMetricBtn)

        // Handle button click
        chooseExerciseButton.setOnClickListener {
            // Show add exercise dialog
            listener?.onChooseActivityExercise()
            dismiss() // Close the dialog
        }
        chooseMetricButton.setOnClickListener {
            // Show add exercise dialog
            listener?.onChooseActivityMetric()
            dismiss() // Close the dialog
        }

        return view
    }

    // Ensure the activity implements the interface
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ChooseActivityTypeListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement ChooseActivityTypeListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null // Avoid memory leaks
    }
}
