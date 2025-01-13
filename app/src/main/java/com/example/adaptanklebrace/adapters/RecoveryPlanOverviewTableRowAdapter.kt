package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.SettingsActivity
import com.example.adaptanklebrace.data.Exercise

class RecoveryPlanOverviewTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val mainActivityCallback: MainActivityCallback
) : RecyclerView.Adapter<RecoveryPlanOverviewTableRowAdapter.ExerciseViewHolder>(), RecoveryPlanAdapter {

    // Define the callback interface
    interface MainActivityCallback {
        fun saveCurrentDateExerciseData()
        fun onFocusFrequencyText(exercise: Exercise)
        fun onClickStartExerciseWithoutWarning(exercise: Exercise)
    }

    // ViewHolder for both header and item rows
    inner class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These will be either EditText or TextView based on row type
        val exerciseName: View = view.findViewById(R.id.exerciseName)
        val sets: View = view.findViewById(R.id.sets)
        val reps: View = view.findViewById(R.id.reps)
        val hold: View = view.findViewById(R.id.hold)
        val tension: View = view.findViewById(R.id.tension)
        val frequency: View = view.findViewById(R.id.frequency)
        val percentageCompleted: View = view.findViewById(R.id.percentageCompleted)
        val startExerciseButton: View = view.findViewById(R.id.startExerciseBtn)

        // Bind method will update based on the view type
        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("DefaultLocale")
        fun bind(exercise: Exercise?, viewType: Int) {
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = "Exercise Name"
                (sets as? TextView)?.text = "Sets"
                (reps as? TextView)?.text = "Reps"
                (hold as? TextView)?.text = "Hold (secs)"
                (tension as? TextView)?.text = "Tension"
                (frequency as? TextView)?.text = "Freq."
                (percentageCompleted as? TextView)?.text = "% Completed"
                (startExerciseButton as? TextView)?.text = "Start Exercise"
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? EditText)?.setText(exercise?.sets.toString())
                (reps as? EditText)?.setText(exercise?.reps.toString())
                (hold as? EditText)?.setText(exercise?.hold.toString())
                (tension as? EditText)?.setText(exercise?.tension.toString())
                (frequency as? EditText)?.setText(exercise?.frequency)
                (percentageCompleted as? TextView)?.text = String.format("%.2f%%", exercise?.percentageCompleted)
                (startExerciseButton as? Button)?.text = "Start"

                // Update color of startExerciseButton based on percentageCompleted field
                if (exercise != null) {
                    // todo: if the exercise name does not exist in the catalog of exercise types, then start button should be hidden
                    if (exercise.percentageCompleted >= 100) {
                        (startExerciseButton as? Button)?.apply {
                            setBackgroundColor(context.getColor(R.color.grey_1))
                        }
                    } else {
                        (startExerciseButton as? Button)?.apply {
                            if (SettingsActivity.nightMode) {
                                setBackgroundColor(context.getColor(R.color.nightPrimary))
                            } else {
                                setBackgroundColor(context.getColor(R.color.lightPrimary))
                            }
                        }
                    }
                }

                // Set up listeners for editable fields
                (sets as? EditText)?.addTextChangedListener {
                    exercise?.sets = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (reps as? EditText)?.addTextChangedListener {
                    exercise?.reps = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (hold as? EditText)?.addTextChangedListener {
                    exercise?.hold = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (tension as? EditText)?.apply {
                    var initialTension: Int? = null  // Variable to store the original tension value

                    // Add TextChangedListener to handle real-time changes to the text
                    addTextChangedListener {
                        val currentTension = text.toString().toIntOrNull()

                        // Restrict tension level between 1-10
                        if (currentTension == null || currentTension !in 1..10) {
                            Toast.makeText(itemView.context, "Please enter a tension level between 1 and 10.", Toast.LENGTH_SHORT).show()
                        } else {
                            exercise?.tension = currentTension
                        }
                        markAsChanged()
                    }

                    // Add OnFocusChangeListener to handle focus loss and reset value if invalid
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            // Store the original tension value when the field gains focus
                            initialTension = exercise?.tension
                        } else {
                            val currentTension = text.toString().toIntOrNull()

                            // If the input is invalid, reset to the original tension value
                            if (currentTension !in 1..10) {
                                // Use the initial value when focus was first gained
                                initialTension?.let {
                                    exercise?.tension = it
                                    setText(it.toString())  // Reset the text to the original tension
                                }
                            }
                            markAsChanged()
                        }
                    }
                }
                (frequency as? EditText)?.apply {
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            exercise?.let {
                                mainActivityCallback.onFocusFrequencyText(it)
                            }
                        }
                    }
                }
                (startExerciseButton as? Button)?.setOnClickListener {
                    exercise?.let {
                        if (it.percentageCompleted >= 100) {
                            // todo: hide the row in the table
                        } else {
                            mainActivityCallback.onClickStartExerciseWithoutWarning(it)
                        }
                    }
                }
            }
        }

        private fun markAsChanged() {
            // This can be used to flag that a change occurred and data needs saving.
            mainActivityCallback.saveCurrentDateExerciseData()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context).inflate(R.layout.recovery_plan_overview_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.recovery_plan_overview_row_item, parent, false)
        }
        return ExerciseViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) { // First position is the header
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        if (position == 0) {
            holder.bind(null, VIEW_TYPE_HEADER) // No exercise data for header
        } else {
            holder.bind(exercises[position - 1], VIEW_TYPE_ITEM) // Bind data for exercise rows
        }
    }

    override fun getItemCount(): Int = exercises.size + 1 // +1 for the header row

    // Constants for view types
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    // Add exercise row to the list
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun addExerciseRow(exercise: Exercise) {
        exercises.add(exercise)
        notifyItemInserted(exercises.size) // Notify adapter
    }

    // Get the current list of exercises
    override fun getExercises(): List<Exercise> {
        return exercises.toList() // Return a copy of the list to avoid external modifications
    }

    // Set a new list of exercises
    override fun setExercises(newExercises: List<Exercise>) {
        exercises.clear()
        exercises.addAll(newExercises)
        refreshTable()
    }

    override fun notifyItemChangedAndRefresh(position: Int) {
        super.notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshTable() {
        notifyDataSetChanged() // Notify adapter
    }
}
