package com.example.adaptanklebrace.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryPlanActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType

@RequiresApi(Build.VERSION_CODES.Q)
class AddExerciseGoalRowFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.add_exercise_goal_row_fragment, container, false)

        // References to fields
        val exerciseNameDropdown: AutoCompleteTextView = view.findViewById(R.id.exerciseNameDropdown)
        val numSetsInput: EditText = view.findViewById(R.id.numSetsInput)
        val numRepsInput: EditText = view.findViewById(R.id.numRepsInput)
        val holdDurationInput: EditText = view.findViewById(R.id.holdDurationInput)
        val tensionLevelInput: EditText = view.findViewById(R.id.tensionLevelInput)
        val freqNumberInput: EditText = view.findViewById(R.id.freqNumberInput)
        val freqCategorySpinner: Spinner = view.findViewById(R.id.freqCategorySpinner)
        val addExerciseGoalButton: Button = view.findViewById(R.id.addExerciseGoalBtn)

        val exerciseNames = ExerciseType.getAllExerciseNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
        exerciseNameDropdown.setAdapter(adapter)

        // Handle button click
        addExerciseGoalButton.setOnClickListener {
            val exerciseName = exerciseNameDropdown.text.toString().trim()
            val numSets = numSetsInput.text.toString().toIntOrNull()
            val numReps = numRepsInput.text.toString().toIntOrNull()
            val holdDuration = holdDurationInput.text.toString().toIntOrNull()
            val tensionLevel = tensionLevelInput.text.toString().toIntOrNull()
            val freqNumber = freqNumberInput.text.toString().toIntOrNull()
            val freqCategory = freqCategorySpinner.selectedItem?.toString()

            if (exerciseName.isEmpty()) {
                showToast("Please enter an exercise name.")
            } else if (numSets == null || numSets <= 0) {
                showToast("Please enter a valid number of sets.")
            } else if (numReps == null || numReps <= 0) {
                showToast("Please enter a valid number of reps.")
            } else if (holdDuration == null || holdDuration < 0) {
                showToast("Please enter a valid hold duration.")
            } else if (tensionLevel == null || tensionLevel !in 1..10) {
                showToast("Please enter a tension level between 1 and 10.")
            } else if (freqNumber == null || freqNumber <= 0) {
                showToast("Please enter a valid frequency number.")
            } else if (freqCategory.isNullOrEmpty()) {
                showToast("Please select a frequency category.")
            } else {
                // Create the exercise object
                val exercise = Exercise(
                    name = exerciseName,
                    sets = numSets,
                    reps = numReps,
                    hold = holdDuration,
                    tension = tensionLevel,
                    frequency = freqNumber.toString() + "x/" + freqCategory
                )

                // Call the RecoveryPlanActivity function to add the goal
                val recoveryPlanActivity = activity as? RecoveryPlanActivity
                recoveryPlanActivity?.onAddRow(exercise)
                dismiss() // Close the dialog
            }
        }

        return view
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
