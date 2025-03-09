package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryPlanActivity
import com.example.adaptanklebrace.adapters.RecoveryExerciseAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

class AddExerciseGoalRowFragment(
    private val context: Context,
    private val exerciseAdapter: RecoveryExerciseAdapter
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_exercise_goal_row, container, false)

        // References to fields
        val exerciseNameDropdown: AutoCompleteTextView = view.findViewById(R.id.exerciseNameDropdown)
        val numSetsInput: EditText = view.findViewById(R.id.numSetsInput)
        val numRepsInput: EditText = view.findViewById(R.id.numRepsInput)
        val holdDurationInput: EditText = view.findViewById(R.id.holdDurationInput)
        val tensionLevelInput: EditText = view.findViewById(R.id.tensionLevelInput)
        val freqNumberInput: EditText = view.findViewById(R.id.freqNumberInput)
        val freqCategorySpinner: Spinner = view.findViewById(R.id.freqCategorySpinner)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val addExerciseGoalButton: Button = view.findViewById(R.id.addExerciseGoalBtn)

        // Initialize exercise name dropdown
        val exerciseNames = ExerciseType.getAllExerciseNames()
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, exerciseNames)
        exerciseNameDropdown.setAdapter(adapter)

        // Show all suggestions when the field gains focus
        exerciseNameDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                exerciseNameDropdown.showDropDown()
            }
        }

        // Handle button click
        addExerciseGoalButton.setOnClickListener {
            val exerciseName = exerciseNameDropdown.text.toString().trim()
            val numSets = numSetsInput.text.toString().toIntOrNull()
            val numReps = numRepsInput.text.toString().toIntOrNull()
            val holdDuration = holdDurationInput.text.toString().toIntOrNull()
            val tensionLevel = tensionLevelInput.text.toString().toIntOrNull()
            val freqNumber = freqNumberInput.text.toString().toIntOrNull()
            val freqCategory = freqCategorySpinner.selectedItem?.toString()
            val comments = commentsInput.text.toString()

            val recoveryPlanActivity = activity as? RecoveryPlanActivity

            // Get the list of exercise goals currently in the table
            val exerciseGoals = recoveryPlanActivity?.getExerciseGoals(exerciseAdapter) ?: listOf()

            //todo: add option to add goal for current week or all future weeks or choose weeks

            if (exerciseName.isEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterExerciseNameToast))
            } else if (exerciseGoals.any { exercise: Exercise ->
                    exercise.name.equals(exerciseName, ignoreCase = true)
                }) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.exerciseAlreadyExistsToast))
            } else if (numSets == null || numSets <= 0) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidSetsToast))
            } else if (numReps == null || numReps <= 0) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidRepsToast))
            } else if (holdDuration == null || holdDuration < 0) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidHoldToast))
            } else if (tensionLevel == null || tensionLevel !in 1..5) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidTensionToast))
            } else if (freqNumber == null || freqNumber <= 0) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidFrequencyNumberToast))
            } else if (freqCategory.isNullOrEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.selectFrequencyCategoryToast))
            } else {
                // Create the exercise object
                val exercise = Exercise(
                    id = ExerciseUtil.generateNewExerciseId(exerciseGoals),
                    name = exerciseName,
                    sets = numSets,
                    reps = numReps,
                    hold = holdDuration,
                    tension = tensionLevel,
                    frequency = freqNumber.toString() + "x/" + freqCategory,
                    comments = comments
                )

                // Call the RecoveryPlanActivity function to add the goal
                recoveryPlanActivity?.onAddExerciseRow(exercise)
                dismiss() // Close the dialog
            }
        }

        return view
    }
}
