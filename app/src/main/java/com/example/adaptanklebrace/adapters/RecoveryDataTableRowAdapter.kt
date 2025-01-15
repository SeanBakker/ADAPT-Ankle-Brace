package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getString
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Exercise.CREATOR.formatter
import java.time.LocalTime

class RecoveryDataTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val recoveryDataCallback: RecoveryDataCallback
) : RecyclerView.Adapter<RecoveryDataTableRowAdapter.ExerciseViewHolder>(), RecoveryPlanAdapter {

    // Define the callback interface
    interface RecoveryDataCallback {
        fun saveCurrentDateExerciseData()
    }

    init {
        // Enable stable IDs useful for the RecyclerView to uniquely identify each row
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Header row has a fixed ID of 0, while exercise rows use their unique IDs
        return if (position == 0) {
            Long.MIN_VALUE
        } else {
            exercises[position - 1].id.toLong() // Use the `id` property of an exercise as the stable ID
        }
    }

    // ViewHolder for both header and item rows
    @RequiresApi(Build.VERSION_CODES.Q)
    inner class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These will be either EditText or TextView based on row type
        val exerciseName: View = view.findViewById(R.id.exerciseName)
        val sets: View = view.findViewById(R.id.sets)
        val reps: View = view.findViewById(R.id.reps)
        val hold: View = view.findViewById(R.id.hold)
        val tension: View = view.findViewById(R.id.tension)
        val time: View = view.findViewById(R.id.time)
        val difficulty: View = view.findViewById(R.id.difficulty)
        val comments: View = view.findViewById(R.id.comments)
        private val selectRowCheckBox: View = view.findViewById(R.id.selectRowCheckBox)

        // Bind method will update based on the view type
        fun bind(exercise: Exercise?, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = getString(context, R.string.exerciseName)
                (sets as? TextView)?.text = getString(context, R.string.sets)
                (reps as? TextView)?.text = getString(context, R.string.reps)
                (hold as? TextView)?.text = getString(context, R.string.holdSecs)
                (tension as? TextView)?.text = getString(context, R.string.tension)
                (time as? TextView)?.text = getString(context, R.string.timeOfCompletion)
                (difficulty as? TextView)?.text = getString(context, R.string.difficulty)
                (comments as? TextView)?.text = getString(context, R.string.comments)
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? EditText)?.setText(exercise?.sets.toString())
                (reps as? EditText)?.setText(exercise?.reps.toString())
                (hold as? EditText)?.setText(exercise?.hold.toString())
                (tension as? EditText)?.setText(exercise?.tension.toString())
                (time as? EditText)?.setText(exercise?.timeCompleted?.let { it.format(formatter) } ?: LocalTime.now().format(formatter))
                (difficulty as? EditText)?.setText(exercise?.difficulty.toString())
                (comments as? EditText)?.setText(exercise?.comments)
                (selectRowCheckBox as? CheckBox)?.isChecked = exercise?.isSelected ?: false

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
                (time as? EditText)?.apply {
                    // Store the original time before editing
                    var originalTime = exercise?.timeCompleted?.format(formatter) ?: "00:00"

                    // Handle the focus change (when the user clicks away from the EditText)
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            originalTime = exercise?.timeCompleted?.format(formatter) ?: "00:00"
                        } else {
                            try {
                                // Only parse when focus is lost
                                exercise?.timeCompleted = LocalTime.parse(text.toString(), formatter)
                                markAsChanged()
                            } catch (e: Exception) {
                                // Handle parsing errors
                                Toast.makeText(context, "Time format invalid, please enter time as 'HH:mm'", Toast.LENGTH_SHORT).show()
                                exercise?.timeCompleted = LocalTime.parse(originalTime, formatter)
                                setText(originalTime)
                                markAsChanged()
                            }
                        }
                    }
                }
                (difficulty as? EditText)?.addTextChangedListener {
                    exercise?.difficulty = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (comments as? EditText)?.addTextChangedListener {
                    exercise?.comments = it.toString()
                    markAsChanged()
                }
                (selectRowCheckBox as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                    exercise?.isSelected = isChecked
                    markAsChanged()
                }
            }
        }

        private fun markAsChanged() {
            // This can be used to flag that a change occurred and data needs saving.
            recoveryDataCallback.saveCurrentDateExerciseData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_row_item, parent, false)
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

    // Delete exercise row from the list
    fun deleteExerciseRow() {
        for (i in exercises.size - 1 downTo 0) {
            if (exercises[i].isSelected) {
                exercises.removeAt(i) // Remove the exercise
                notifyItemRemoved(i+1) // Notify adapter
            }
        }
    }

    // Get the current list of exercises
    override fun getExercises(): List<Exercise> {
        return exercises.toList() // Return a copy of the list to avoid external modifications
    }

    // Set a new list of exercises
    override fun setExercises(newExercises: List<Exercise>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyItemRangeChanged(1, getItemCount())
    }

    override fun notifyItemChangedAndRefresh(position: Int) {
        super.notifyItemChanged(position)
        recoveryDataCallback.saveCurrentDateExerciseData()
    }
}
