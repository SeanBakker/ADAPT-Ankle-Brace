package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise

class TensionLevelWarningFragment(
    private val exercise: Exercise,
    private val actualTensionLevel: Int
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tension_level_warning_fragment, container, false)

        val expectedTensionText: TextView = view.findViewById(R.id.expectedTension)
        expectedTensionText.text = exercise.tension.toString()

        val actualTensionText: TextView = view.findViewById(R.id.actualTension)
        actualTensionText.text = actualTensionLevel.toString()

        val startExerciseButton: Button = view.findViewById(R.id.startExerciseBtn)
        startExerciseButton.setOnClickListener {
            startExercise()
        }

        return view
    }

    private fun startExercise() {
        // Trigger fragment for performing test rep
        // todo: implement test rep fragment call

        dismiss() // Close the dialog
    }
}
