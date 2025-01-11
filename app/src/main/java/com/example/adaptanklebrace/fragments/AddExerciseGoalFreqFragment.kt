package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.adapters.RecoveryPlanTableRowAdapter
import com.example.adaptanklebrace.data.Exercise

class AddExerciseGoalFreqFragment(
    private val exerciseAdapter: RecoveryPlanTableRowAdapter,
    private val exercise: Exercise
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_exercise_goal_freq, container, false)

        // References to fields
        val freqNumberInput: EditText = view.findViewById(R.id.freqNumberInput)
        val freqCategorySpinner: Spinner = view.findViewById(R.id.freqCategorySpinner)
        val setFreqButton: Button = view.findViewById(R.id.setFreqBtn)

        // Handle button click
        setFreqButton.setOnClickListener {
            val freqNumber = freqNumberInput.text.toString().toIntOrNull()
            val freqCategory = freqCategorySpinner.selectedItem?.toString()

            if (freqNumber == null || freqNumber <= 0) {
                showToast("Please enter a valid frequency number.")
            } else if (freqCategory.isNullOrEmpty()) {
                showToast("Please select a frequency category.")
            } else {
                // Update the frequency field of the exercise & notify the adapter
                exercise.frequency = freqNumber.toString() + "x/" + freqCategory
                exerciseAdapter.refreshTable()
                dismiss() // Close the dialog
            }
        }

        return view
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
