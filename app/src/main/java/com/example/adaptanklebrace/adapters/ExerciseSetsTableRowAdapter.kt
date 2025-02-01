package com.example.adaptanklebrace.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R

class ExerciseSetsTableRowAdapter(
    private val sets: MutableList<Pair<Int, Int>>, // Pair<Set #, Reps Count>
) : RecyclerView.Adapter<ExerciseSetsTableRowAdapter.SetViewHolder>() {

    // ViewHolder for both header and item rows
    inner class SetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val setNumber: View = view.findViewById(R.id.setNumber)
        private val repsCount: View = view.findViewById(R.id.repsCount)

        // todo: fix bug with overwriting reps counts

        // Bind method will update based on the view type
        fun bind(set: Pair<Int, Int>?, viewType: Int) {
            if (viewType == VIEW_TYPE_HEADER) {
                // Header titles
                (setNumber as? TextView)?.text = "Set #"
                (repsCount as? TextView)?.text = "Reps"
            } else {
                // Row data
                (setNumber as? TextView)?.text = set?.first.toString()
                (repsCount as? EditText)?.setText(set?.second.toString())

                // Set up listeners for editable fields
                (repsCount as? EditText)?.addTextChangedListener {
                    val index = set?.first?.minus(1) ?: 0
                    val reps = it.toString().toIntOrNull() ?: 0
                    sets[index] = Pair(index + 1, reps)
                }
            }
        }
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

    override fun getItemCount(): Int = sets.size + 1 // +1 for the header row

    /**
     * Retrieves the item count of only non-zero reps in the table.
     *
     * @return integer item count
     */
    fun getNonZeroRowsCount(): Int {
        return getSetsWithNonZeroReps().size
    }

    // Constants for view types
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    /**
     * Retrieves the current list of sets with non-zero rep counts.
     *
     * @return list of integer pairs
     */
    private fun getSetsWithNonZeroReps(): List<Pair<Int, Int>> {
        return sets.filter { it.second > 0 }.toList() // Return a copy of the list to avoid external modifications
    }

    /**
     * Creates a new list of sets.
     *
     * @param newSets list of integer pairs
     */
    fun createSets(newSets: List<Pair<Int, Int>>) {
        sets.clear()
        sets.addAll(newSets)
        notifyItemRangeChanged(1, getItemCount())
    }

    /**
     * Retrieves the average number of reps for all sets completed.
     *
     * @return integer item count
     */
    fun getAverageReps(): Int {
        val sets = getSetsWithNonZeroReps()
        if (sets.isNotEmpty()) {
            var repsTotal = 0
            for (set in sets) {
                repsTotal += set.second
            }
            return (repsTotal / sets.size)
        } else {
            return 0
        }
    }
}
