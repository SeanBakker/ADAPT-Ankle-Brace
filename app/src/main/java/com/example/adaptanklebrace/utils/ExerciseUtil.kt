package com.example.adaptanklebrace.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.CalendarDay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class ExerciseUtil {
    companion object {
        // Define a formatter for LocalTime
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /**
         * Generate IDs for existing exercises.
         *
         * @param existingExercises list of all exercises that have already been created
         * @return new Int id unique to all other existing exercises
         */
        fun generateNewId(existingExercises: List<Exercise>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingExercises.map { it.id }) }

            // Get the last (largest) ID in the sorted set, or start from 0 if the set is empty
            val largestId = sortedIds.lastOrNull() ?: 0

            // Return the next ID after the largest existing id
            return largestId + 1
        }

        /**
         * Load the number of sets required for the exercise goal to build the table rows.
         *
         * @param setsAdapter adapter to create sets for
         * @param exerciseGoal exercise goal to be performed
         */
        fun loadSetsData(setsAdapter: ExerciseSetsTableRowAdapter, exerciseGoal: Exercise) {
            val sets: MutableList<Pair<Int, Int>> = mutableListOf()

            // Create an element in the list for each set
            for (setNumber in 0 until exerciseGoal.sets) {
                sets.add(Pair(setNumber+1, 0))
            }

            // Load the sets for the table
            setsAdapter.createSets(sets)
        }

        /**
         * Show toast messages with custom background and positioning.
         *
         * @param context context of where the message is being sent
         * @param layoutInflater inflater to be able to show the message
         * @param message the message to be displayed
         */
        @SuppressLint("InflateParams")
        fun showToast(context: Context, layoutInflater: LayoutInflater, message: String) {
            // Retrieve the custom toast background
            val inflater = layoutInflater
            val customView = inflater.inflate(R.layout.custom_toast, null)
            val customToast = customView.findViewById<TextView>(R.id.toast_text)
            customToast.text = message

            val toast = Toast(context)
            toast.duration = Toast.LENGTH_SHORT
            toast.setGravity(Gravity.TOP, 0, 10)
            toast.view = customView
            toast.show()
        }

        /**
         * Show the date picker dialog for selecting a day of the week.
         *
         * @param context context of which to build the alert
         * @param dateInput textview of which to update the selected day
         * @param onDaySelected callback to the chosen date selected
         */
        fun showDayPickerDialog(context: Context, dateInput: TextView, onDaySelected: (String) -> Unit = {}) {
            // Define an array of days of the week
            val daysOfWeek = CalendarDay.getAllDayNames().toTypedArray()

            // Create an AlertDialog to allow the user to select a day of the week
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Select a day of the week")
            builder.setItems(daysOfWeek) { _, chosenDay ->
                // Get the selected day
                val selectedDay = daysOfWeek[chosenDay]

                // Set the selected day to the TextView
                dateInput.text = selectedDay

                // Trigger the callback
                onDaySelected(selectedDay)
            }

            // Show the dialog
            builder.show()
        }

        /**
         * Show time picker dialog for selecting times.
         *
         * @param context context of which to show the dialog
         * @param timeInput textview of which to update the selected time
         * @param onTimeSelected callback to the chosen time selected
         */
        fun showTimePickerDialog(context: Context, timeInput: TextView, onTimeSelected: (LocalTime) -> Unit = {}) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    val localTimeString = updateTimeInput(timeInput, calendar)

                    // Trigger the callback
                    val localTime = LocalTime.parse(localTimeString, timeFormatter)
                    onTimeSelected(localTime)
                },
                hour, minute, true
            ).show()
        }

        /**
         * Set time input to the selected time.
         *
         * @param timeInput textview of which to update the selected time
         * @param calendar calendar of which to get the selected time
         */
        fun updateTimeInput(timeInput: TextView, calendar: Calendar): String {
            timeInput.text = Converters.convertDateToString(calendar.time)
            return timeInput.text.toString()
        }
    }
}
