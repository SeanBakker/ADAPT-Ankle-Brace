package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise

class ExerciseTableRowAdapter(
    private val context: Context,
    private val exercises: MutableList<Exercise>,
    private val saveDataCallback: SaveDataCallback
) : RecyclerView.Adapter<ExerciseTableRowAdapter.ExerciseViewHolder>() {

    // Define the callback interface
    interface SaveDataCallback {
        fun saveCurrentDateData()
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
        val difficulty: View = view.findViewById(R.id.difficulty)
        val comments: View = view.findViewById(R.id.comments)
        val startCheckBox: View = view.findViewById(R.id.startCheckBox)

        // Bind method will update based on the view type
        fun bind(exercise: Exercise?, viewType: Int) {
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = "Exercise Name"
                (sets as? TextView)?.text = "Sets"
                (reps as? TextView)?.text = "Reps"
                (hold as? TextView)?.text = "Hold"
                (tension as? TextView)?.text = "Tension"
                (frequency as? TextView)?.text = "Frequency"
                (difficulty as? TextView)?.text = "Difficulty"
                (comments as? TextView)?.text = "Comments"
                (startCheckBox as? TextView)?.text = "Started?"
                // Disable startCheckBox in the header row
                //(startCheckBox as? CheckBox)?.visibility = View.GONE
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? EditText)?.setText(exercise?.sets.toString())
                (reps as? EditText)?.setText(exercise?.reps.toString())
                (hold as? EditText)?.setText(exercise?.hold.toString())
                (tension as? EditText)?.setText(exercise?.tension.toString())
                (frequency as? EditText)?.setText(exercise?.frequency)
                (difficulty as? EditText)?.setText(exercise?.difficulty.toString())
                (comments as? EditText)?.setText(exercise?.comments)
                (startCheckBox as? CheckBox)?.isChecked = exercise?.isStarted ?: false

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
                (tension as? EditText)?.addTextChangedListener {
                    exercise?.tension = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (frequency as? EditText)?.addTextChangedListener {
                    exercise?.frequency = it.toString()
                    markAsChanged()
                }
                (difficulty as? EditText)?.addTextChangedListener {
                    exercise?.difficulty = it.toString().toIntOrNull() ?: 0
                    markAsChanged()
                }
                (comments as? EditText)?.addTextChangedListener {
                    exercise?.comments = it.toString()
                    markAsChanged()
                }
                (startCheckBox as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                    exercise?.isStarted = isChecked
                    markAsChanged()
                }
            }
        }

        private fun markAsChanged() {
            // This can be used to flag that a change occurred and data needs saving.
            // For example, update a variable that tracks changes or call the save function directly
            saveDataCallback.saveCurrentDateData()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context).inflate(R.layout.exercise_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.exercise_row_item, parent, false)
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
    fun addExerciseRow(exerciseName: String) {
        exercises.add(Exercise(name = exerciseName)) // Add a blank row with default values
        notifyItemInserted(exercises.size) // Insert the new exercise row
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
        notifyDataSetChanged() // Notify that data has changed
    }
}
