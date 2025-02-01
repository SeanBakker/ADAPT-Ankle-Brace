package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryDataActivity
import com.example.adaptanklebrace.adapters.RecoveryPlanAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import java.time.LocalTime
import java.util.Calendar

class AddExerciseDataRowFragment(
    private val context: Context,
    private val exerciseAdapter: RecoveryPlanAdapter
) : DialogFragment() {

    private lateinit var timeInput: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_exercise_data_row, container, false)

        // References to fields
        val exerciseNameDropdown: AutoCompleteTextView = view.findViewById(R.id.exerciseNameDropdown)
        val numSetsInput: EditText = view.findViewById(R.id.numSetsInput)
        val numRepsInput: EditText = view.findViewById(R.id.numRepsInput)
        val holdDurationInput: EditText = view.findViewById(R.id.holdDurationInput)
        val tensionLevelInput: EditText = view.findViewById(R.id.tensionLevelInput)
        timeInput = view.findViewById(R.id.timeInput)
        val difficultyLevelInput: EditText = view.findViewById(R.id.difficultyLevelInput)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val addExerciseDataButton: Button = view.findViewById(R.id.addExerciseDataBtn)

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

        // Set default time to the current time
        GeneralUtil.updateTimeInput(timeInput, Calendar.getInstance())

        // Handle time input click
        timeInput.setOnClickListener {
            GeneralUtil.showTimePickerDialog(context, timeInput)
        }

        // Handle button click
        addExerciseDataButton.setOnClickListener {
            val exerciseName = exerciseNameDropdown.text.toString().trim()
            val numSets = numSetsInput.text.toString().toIntOrNull()
            val numReps = numRepsInput.text.toString().toIntOrNull()
            val holdDuration = holdDurationInput.text.toString().toIntOrNull()
            val tensionLevel = tensionLevelInput.text.toString().toIntOrNull()
            val timeOfCompletion = timeInput.text.toString()
            val difficultyLevel = difficultyLevelInput.text.toString().toIntOrNull()
            val comments = commentsInput.text.toString()

            //todo: add option to pick dates for which the exercise was completed for

            if (exerciseName.isEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter an exercise name.")
            } else if (numSets == null || numSets <= 0) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a valid number of sets.")
            } else if (numReps == null || numReps <= 0) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a valid number of reps.")
            } else if (holdDuration == null || holdDuration < 0) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a valid hold duration.")
            } else if (tensionLevel == null || tensionLevel !in 1..10) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a tension level between 1 and 10.")
            } else if (difficultyLevel != null && difficultyLevel !in 1..10) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a difficulty level between 1 and 10, or leave it blank.")
            } else {
                val exercises = exerciseAdapter.getExercises()

                // Create the exercise object
                val exercise = Exercise(
                    id = ExerciseUtil.generateNewId(exercises),
                    name = exerciseName,
                    sets = numSets,
                    reps = numReps,
                    hold = holdDuration,
                    tension = tensionLevel,
                    timeCompleted = LocalTime.parse(timeOfCompletion, GeneralUtil.timeFormatter),
                    difficulty = difficultyLevel ?: 0,
                    comments = comments
                )

                // Add the exercise to RecoveryDataActivity
                val recoveryDataActivity = activity as? RecoveryDataActivity
                recoveryDataActivity?.onAddRow(exercise)
                dismiss() // Close the dialog
            }
        }

        return view
    }
}
