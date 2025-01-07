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
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Exercise.CREATOR.formatter
import java.time.LocalTime

class RecoveryDataTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val saveDataCallback: SaveDataCallback
) : RecyclerView.Adapter<RecoveryDataTableRowAdapter.ExerciseViewHolder>() {

    // Define the callback interface
    interface SaveDataCallback {
        fun saveCurrentDateExerciseData()
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
        val selectRowCheckBox: View = view.findViewById(R.id.selectRowCheckBox)

        // Bind method will update based on the view type
        fun bind(exercise: Exercise?, viewType: Int) {
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = "Exercise Name"
                (sets as? TextView)?.text = "Sets"
                (reps as? TextView)?.text = "Reps"
                (hold as? TextView)?.text = "Hold (secs)"
                (tension as? TextView)?.text = "Tension"
                (time as? TextView)?.text = "Time of Completion"
                (difficulty as? TextView)?.text = "Difficulty"
                (comments as? TextView)?.text = "Comments"
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
            saveDataCallback.saveCurrentDateExerciseData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context).inflate(R.layout.recovery_data_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.recovery_data_row_item, parent, false)
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
    fun addExerciseRow(exercise: Exercise) {
        exercises.add(exercise)
        notifyItemInserted(exercises.size) // Notify adapter
    }

    // Delete exercise row from the list
    @SuppressLint("NotifyDataSetChanged")
    fun deleteExerciseRow() {
        for (i in exercises.size - 1 downTo 0) {
            if (exercises[i].isSelected) {
                exercises.removeAt(i) // Remove the exercise
            }
        }
        notifyDataSetChanged() // Notify adapter
    }

    // Get the current list of exercises
    fun getExercises(): List<Exercise> {
        return exercises.toList() // Return a copy of the list to avoid external modifications
    }

    // Set a new list of exercises
    @SuppressLint("NotifyDataSetChanged")
    fun setExercises(newExercises: List<Exercise>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyDataSetChanged() // Notify adapter
    }
}
