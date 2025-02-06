package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
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
import com.example.adaptanklebrace.SettingsActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.GeneralUtil

class RecoveryPlanExerciseTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val recoveryPlanCallback: RecoveryPlanCallback
) : RecyclerView.Adapter<RecoveryPlanExerciseTableRowAdapter.ExerciseViewHolder>(), RecoveryExerciseAdapter {

    // Define the callback interface
    interface RecoveryPlanCallback {
        fun saveCurrentDateExerciseData()
        fun onFocusFrequencyText(exercise: Exercise, position: Int)
        fun onClickStartExerciseWithWarning(exercise: Exercise)
        fun onClickStartExerciseWithoutWarning(exercise: Exercise)
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
        val frequency: View = view.findViewById(R.id.frequency)
        private val percentageCompleted: View = view.findViewById(R.id.percentageCompleted)
        private val startExerciseButton: View = view.findViewById(R.id.startExerciseBtn)
        val comments: View = view.findViewById(R.id.comments)
        private val selectRowCheckBox: View = view.findViewById(R.id.selectRowCheckBox)

        // Bind method will update based on the view type
        @SuppressLint("DefaultLocale")
        fun bind(exercise: Exercise?, position: Int, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = getString(context, R.string.exerciseName)
                (sets as? TextView)?.text = getString(context, R.string.sets)
                (reps as? TextView)?.text = getString(context, R.string.reps)
                (hold as? TextView)?.text = getString(context, R.string.holdSecs)
                (tension as? TextView)?.text = getString(context, R.string.tension)
                (frequency as? TextView)?.text = getString(context, R.string.freq)
                (percentageCompleted as? TextView)?.text =
                    getString(context, R.string.percentCompleted)
                (startExerciseButton as? TextView)?.text =
                    getString(context, R.string.startExerciseBtn)
                (comments as? TextView)?.text = getString(context, R.string.comments)
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? EditText)?.setText(exercise?.sets.toString())
                (reps as? EditText)?.setText(exercise?.reps.toString())
                (hold as? EditText)?.setText(exercise?.hold.toString())
                (tension as? EditText)?.setText(exercise?.tension.toString())
                (frequency as? EditText)?.setText(exercise?.frequency)
                (percentageCompleted as? TextView)?.text =
                    String.format("%.0f%%", exercise?.percentageCompleted)
                (startExerciseButton as? Button)?.text = getString(context, R.string.startBtn)
                (comments as? EditText)?.setText(exercise?.comments)
                (selectRowCheckBox as? CheckBox)?.isChecked = exercise?.isSelected ?: false

                // Update color of startExerciseButton
                if (exercise != null) {
                    if (exercise.percentageCompleted >= 100 || !ExerciseType.getAllExerciseNames().contains(exercise.name)) {
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

                        // Restrict tension level between 1-10
                        if (currentTension == null || currentTension !in 1..10) {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), "Please enter a tension level between 1 and 10.")
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
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            exercise?.let {
                                recoveryPlanCallback.onFocusFrequencyText(it, position)
                                frequency.clearFocus()
                            }
                        }
                    }
                }
                (startExerciseButton as? Button)?.setOnClickListener {
                    exercise?.let {
                        if (it.percentageCompleted >= 100) {
                            recoveryPlanCallback.onClickStartExerciseWithWarning(it)
                        } else {
                            recoveryPlanCallback.onClickStartExerciseWithoutWarning(it)
                        }
                    }
                }
                (comments as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        exercise?.comments = it.toString()
                        markAsChanged()
                    }
                    tag = newWatcher
                }
                (selectRowCheckBox as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                    exercise?.isSelected = isChecked
                    markAsChanged()
                }
            }
        }

        private fun markAsChanged() {
            // This can be used to flag that a change occurred and data needs saving.
            recoveryPlanCallback.saveCurrentDateExerciseData()
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
                .inflate(R.layout.recovery_plan_exercise_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_plan_exercise_row_item, parent, false)
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
            holder.bind(null, 0, VIEW_TYPE_HEADER) // No exercise data for header
        } else if (position != RecyclerView.NO_POSITION) {
            holder.bind(exercises[position - 1], position, VIEW_TYPE_ITEM) // Bind data for exercise rows
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
        (holder.comments as? EditText)?.apply {
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
        recoveryPlanCallback.saveCurrentDateExerciseData()
    }
}
