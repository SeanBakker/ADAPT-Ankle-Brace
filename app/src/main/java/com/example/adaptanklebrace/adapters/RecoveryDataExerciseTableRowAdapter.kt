package com.example.adaptanklebrace.adapters

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.utils.GeneralUtil
import java.time.LocalTime

class RecoveryDataExerciseTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val recoveryDataCallback: RecoveryDataCallback
) : RecyclerView.Adapter<RecoveryDataExerciseTableRowAdapter.ExerciseViewHolder>(), RecoveryExerciseAdapter {

    // Define the callback interface
    interface RecoveryDataCallback {
        fun saveCurrentDateExerciseData()
        fun onClickViewExerciseDetails(exercise: Exercise, view: View)
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
    inner class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These will be either EditText or TextView based on row type
        val exerciseName: View = view.findViewById(R.id.exerciseName)
        val sets: View = view.findViewById(R.id.sets)
        val reps: View = view.findViewById(R.id.reps)
        val hold: View = view.findViewById(R.id.hold)
        val tension: View = view.findViewById(R.id.tension)
        val time: View = view.findViewById(R.id.time)
        val difficulty: View = view.findViewById(R.id.difficulty)
        private val viewDetailsButton: View = view.findViewById(R.id.viewDetailsBtn)
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
                (viewDetailsButton as? TextView)?.text = getString(context, R.string.viewDetails)
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? EditText)?.setText(exercise?.sets.toString())
                (reps as? EditText)?.setText(exercise?.reps.toString())
                (hold as? EditText)?.setText(exercise?.hold.toString())
                (tension as? EditText)?.setText(exercise?.tension.toString())
                (time as? EditText)?.setText(
                    exercise?.timeCompleted?.format(GeneralUtil.timeFormatter) ?: LocalTime.now().format(GeneralUtil.timeFormatter))
                (difficulty as? EditText)?.setText(exercise?.difficulty.toString())
                (viewDetailsButton as? Button)?.text = getString(context, R.string.viewBtn)
                (selectRowCheckBox as? CheckBox)?.isChecked = exercise?.isSelected ?: false

                // Set up listeners for editable fields
                (sets as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        exercise?.sets = it.toString().toIntOrNull() ?: 0
                        markAsChanged()
                    }
                    tag = newWatcher
                }
                (reps as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        exercise?.reps = it.toString().toIntOrNull() ?: 0
                        markAsChanged()
                    }
                    tag = newWatcher
                }
                (hold as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        exercise?.hold = it.toString().toIntOrNull() ?: 0
                        markAsChanged()
                    }
                    tag = newWatcher
                }
                (tension as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    var initialTension: Int? = null  // Variable to store the original tension value

                    // Add TextChangedListener to handle real-time changes to the text
                    val newWatcher = addTextChangedListener {
                        val currentTension = text.toString().toIntOrNull()

                        // Restrict tension level between 1-5
                        if (currentTension == null || currentTension !in 1..5) {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), context.getString(R.string.enterValidTensionToast))
                        } else {
                            exercise?.tension = currentTension
                        }
                        markAsChanged()
                    }
                    tag = newWatcher

                    // Add OnFocusChangeListener to handle focus loss and reset value if invalid
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            // Store the original tension value when the field gains focus
                            initialTension = exercise?.tension
                        } else {
                            val currentTension = text.toString().toIntOrNull()

                            // If the input is invalid, reset to the original tension value
                            if (currentTension !in 1..5) {
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
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            exercise?.let {
                                GeneralUtil.showTimePickerDialog(context, this) { selectedTime ->
                                    exercise.timeCompleted = selectedTime
                                    markAsChanged()
                                }
                                time.clearFocus()
                            }
                        }
                    }
                }
                (difficulty as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    var initialDifficulty: Int? = null  // Variable to store the original difficulty value

                    // Add TextChangedListener to handle real-time changes to the text
                    val newWatcher = addTextChangedListener {
                        val currentDifficulty = text.toString().toIntOrNull()

                        // Restrict difficulty level between 0-10
                        if (currentDifficulty == null || currentDifficulty !in 0..10) {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), context.getString(R.string.enterDifficultyLevelToast))
                        } else {
                            exercise?.difficulty = currentDifficulty
                        }
                        markAsChanged()
                    }
                    tag = newWatcher

                    // Add OnFocusChangeListener to handle focus loss and reset value if invalid
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            // Store the original difficulty value when the field gains focus
                            initialDifficulty = exercise?.difficulty
                        } else {
                            val currentdifficulty = text.toString().toIntOrNull()

                            // If the input is invalid, reset to the original difficulty value
                            if (currentdifficulty !in 0..10) {
                                // Use the initial value when focus was first gained
                                initialDifficulty?.let {
                                    exercise?.difficulty = it
                                    setText(it.toString())  // Reset the text to the original difficulty
                                }
                            }
                            markAsChanged()
                        }
                    }
                }
                (viewDetailsButton as? Button)?.setOnClickListener {
                    exercise?.let {
                        recoveryDataCallback.onClickViewExerciseDetails(it, viewDetailsButton)
                    }
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

    // Constants for view types
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_exercise_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_exercise_row_item, parent, false)
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

    // Ensure listeners are properly cleared when view is detached from the window
    override fun onViewRecycled(holder: ExerciseViewHolder) {
        super.onViewRecycled(holder)
        (holder.sets as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
        (holder.reps as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
        (holder.hold as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
        (holder.tension as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
        (holder.difficulty as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
    }

    override fun getItemCount(): Int = exercises.size + 1 // +1 for the header row

    // Add exercise row to the list
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
        // Clear the existing data
        val previousSize = getItemCount()
        exercises.clear()
        notifyItemRangeRemoved(1, previousSize)

        // Add new exercises
        exercises.addAll(newExercises)
        notifyItemRangeInserted(1, getItemCount())
    }

    override fun notifyItemChangedAndRefresh(position: Int) {
        super.notifyItemChanged(position)
        recoveryDataCallback.saveCurrentDateExerciseData()
    }
}
