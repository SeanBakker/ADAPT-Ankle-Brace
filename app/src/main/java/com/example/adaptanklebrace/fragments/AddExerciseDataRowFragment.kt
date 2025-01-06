package com.example.adaptanklebrace.fragments

import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryDataActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Exercise.CREATOR.formatter
import com.example.adaptanklebrace.enums.ExerciseType
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.Q)
class AddExerciseDataRowFragment : DialogFragment() {

    private lateinit var timeInput: TextView
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.add_exercise_data_row_fragment, container, false)

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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exerciseNames)
        exerciseNameDropdown.setAdapter(adapter)

        // Show all suggestions when the field gains focus
        exerciseNameDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                exerciseNameDropdown.showDropDown()
            }
        }

        // Set default time to the current time
        updateTimeInput()

        // Handle time input click
        timeInput.setOnClickListener {
            showTimePickerDialog()
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
                showToast("Please enter an exercise name.")
            } else if (numSets == null || numSets <= 0) {
                showToast("Please enter a valid number of sets.")
            } else if (numReps == null || numReps <= 0) {
                showToast("Please enter a valid number of reps.")
            } else if (holdDuration == null || holdDuration < 0) {
                showToast("Please enter a valid hold duration.")
            } else if (tensionLevel == null || tensionLevel !in 1..10) {
                showToast("Please enter a tension level between 1 and 10.")
            } else if (difficultyLevel != null && difficultyLevel !in 1..10) {
                showToast("Please enter a difficulty level between 1 and 10, or leave it blank.")
            } else {
                // Create the exercise object
                val exercise = Exercise(
                    name = exerciseName,
                    sets = numSets,
                    reps = numReps,
                    hold = holdDuration,
                    tension = tensionLevel,
                    timeCompleted = LocalTime.parse(timeOfCompletion, formatter),
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

    private fun updateTimeInput() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeInput.text = timeFormat.format(calendar.time)
    }

    private fun showTimePickerDialog() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateTimeInput()
        }, hour, minute, true).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
