package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.SettingsActivity
import com.example.adaptanklebrace.data.Exercise
import androidx.core.content.ContextCompat.getString
import com.example.adaptanklebrace.enums.ExerciseType

class RecoveryPlanOverviewTableRowAdapter(
    private val exercises: MutableList<Exercise>,
    private val mainActivityCallback: MainActivityCallback
) : RecyclerView.Adapter<RecoveryPlanOverviewTableRowAdapter.ExerciseViewHolder>(), RecoveryPlanAdapter {

    // Define the callback interface
    interface MainActivityCallback {
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

        // Bind method will update based on the view type
        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("DefaultLocale")
        fun bind(exercise: Exercise?, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (exerciseName as? TextView)?.text = getString(context, R.string.exerciseName)
                (sets as? TextView)?.text = getString(context, R.string.sets)
                (reps as? TextView)?.text = getString(context, R.string.reps)
                (hold as? TextView)?.text = getString(context, R.string.holdSecs)
                (tension as? TextView)?.text = getString(context, R.string.tension)
                (frequency as? TextView)?.text = getString(context, R.string.freq)
                (percentageCompleted as? TextView)?.text = getString(context, R.string.percentCompleted)
                (startExerciseButton as? TextView)?.text = getString(context, R.string.startExerciseBtn)
            } else {
                // Bind editable fields for exercise data rows
                (exerciseName as? TextView)?.text = exercise?.name
                (sets as? TextView)?.text = exercise?.sets.toString()
                (reps as? TextView)?.text = exercise?.reps.toString()
                (hold as? TextView)?.text = exercise?.hold.toString()
                (tension as? TextView)?.text = exercise?.tension.toString()
                (frequency as? TextView)?.text = exercise?.frequency
                (percentageCompleted as? TextView)?.text = String.format("%.2f%%", exercise?.percentageCompleted)
                (startExerciseButton as? Button)?.text = getString(context, R.string.startBtn)

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

                // Set up listeners
                (startExerciseButton as? Button)?.setOnClickListener {
                    exercise?.let {
                        mainActivityCallback.onClickStartExerciseWithoutWarning(it)
                    }
                }
            }
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
                .inflate(R.layout.recovery_plan_overview_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_plan_overview_row_item, parent, false)
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
            val exercise = exercises[position - 1]

            // Hide the exercise row if the percentage completed is >=100
            if (exercise.percentageCompleted >= 100) {
                // Hide the row
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                // Show the row and bind its data
                holder.itemView.visibility = View.VISIBLE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                holder.bind(exercise, VIEW_TYPE_ITEM) // Bind data for exercise rows
            }
        }
    }

    override fun getItemCount(): Int = exercises.size + 1 // +1 for the header row

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
        notifyItemRangeChanged(1, getItemCount())
    }

    override fun notifyItemChangedAndRefresh(position: Int) {
        super.notifyItemChanged(position)
    }
}
