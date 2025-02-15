package com.example.adaptanklebrace.adapters

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.ExerciseSet
import com.example.adaptanklebrace.utils.GeneralUtil

class ExerciseSetsTableRowAdapter(
    private val sets: MutableList<ExerciseSet>,
) : RecyclerView.Adapter<ExerciseSetsTableRowAdapter.SetViewHolder>() {

    init {
        // Enable stable IDs useful for the RecyclerView to uniquely identify each row
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Header row has a fixed ID of 0, while metric rows use their unique IDs
        return if (position == 0) {
            Long.MIN_VALUE
        } else {
            sets[position - 1].id.toLong() // Use the `id` property of a metric as the stable ID
        }
    }

    // ViewHolder for both header and item rows
    inner class SetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val setNumber: View = view.findViewById(R.id.setNumber)
        val repsCount: View = view.findViewById(R.id.repsCount)

        // Bind method will update based on the view type
        fun bind(set: ExerciseSet?, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Header titles
                (setNumber as? TextView)?.text = getString(context, R.string.setNumber)
                (repsCount as? TextView)?.text = getString(context, R.string.reps)
            } else {
                // Row data
                (setNumber as? TextView)?.text = set?.id.toString()
                (repsCount as? EditText)?.setText(set?.reps.toString())

                // Set up listeners for editable fields
                (repsCount as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        val currentReps = it.toString().toIntOrNull()

                        // Restrict reps count to be >0
                        if (currentReps == null || currentReps < 0) {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), "Please enter a reps count greater than or equal to 0.")
                        } else {
                            set?.reps = currentReps
                        }
                    }
                    tag = newWatcher
                }
            }
        }
    }

    // Constants for view types
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context).inflate(R.layout.sets_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.sets_row_item, parent, false)
        }
        return SetViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) { // First position is the header
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        if (position == 0) {
            holder.bind(null, VIEW_TYPE_HEADER) // No set data for header
        } else {
            holder.bind(sets[position - 1], VIEW_TYPE_ITEM) // Bind data for rows
        }
    }

    // Ensure listeners are properly cleared when view is detached from the window
    override fun onViewRecycled(holder: SetViewHolder) {
        super.onViewRecycled(holder)
        (holder.repsCount as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
    }

    override fun getItemCount(): Int = sets.size + 1 // +1 for the header row

    /**
     * Retrieves the item count of only non-zero reps in the table.
     *
     * @return integer item count
     */
    fun getNonZeroRowsCount(): Int {
        return getSetsWithNonZeroReps().size
    }

    /**
     * Retrieves the current list of sets with non-zero rep counts.
     *
     * @return list of ExerciseSets
     */
    private fun getSetsWithNonZeroReps(): List<ExerciseSet> {
        return sets.filter { it.reps > 0 }.toList() // Return a copy of the list to avoid external modifications
    }

    /**
     * Retrieves the first set row with zero reps.
     *
     * @return ExerciseSet row
     */
    fun getNextSetWithZeroReps(): ExerciseSet? {
        return sets.firstOrNull { it.reps == 0 }
    }

    /**
     * Creates a new list of sets.
     *
     * @param newSets list of ExerciseSets
     */
    fun createSets(newSets: List<ExerciseSet>) {
        // Clear the existing data
        val previousSize = getItemCount()
        sets.clear()
        notifyItemRangeRemoved(1, previousSize)

        // Add new metrics
        sets.addAll(newSets)
        notifyItemRangeInserted(1, getItemCount())
    }

    /**
     * Adds a new set row to the existing list of sets.
     *
     * @param set new set row to add
     */
    fun addSetRow(set: ExerciseSet) {
        sets.add(set)
        notifyItemInserted(sets.size) // Notify adapter
    }

    /**
     * Retrieves the average number of reps for all sets completed.
     * Floor the resulting average to an integer.
     *
     * @return integer item count
     */
    fun getAverageReps(): Int {
        val sets = getSetsWithNonZeroReps()
        if (sets.isNotEmpty()) {
            var repsTotal = 0
            for (set in sets) {
                repsTotal += set.reps
            }
            return repsTotal.floorDiv(sets.size)
        } else {
            return 0
        }
    }
}
