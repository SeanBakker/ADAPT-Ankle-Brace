package com.example.adaptanklebrace.utils

import android.graphics.Rect
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryAdapter
import com.example.adaptanklebrace.adapters.RecoveryOverviewAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseSet
import com.example.adaptanklebrace.data.Metric

class ExerciseUtil {
    companion object {
        /**
         * Generate IDs for existing exercises.
         *
         * @param existingExercises list of all exercises that have already been created
         * @return new Int id unique to all other existing exercises
         */
        fun generateNewExerciseId(existingExercises: List<Exercise>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingExercises.map { it.id }) }

            // Get the last (largest) ID in the sorted set, or start from 0 if the set is empty
            val largestId = sortedIds.lastOrNull() ?: 0

            // Return the next ID after the largest existing id
            return largestId + 1
        }

        /**
         * Generate IDs for existing sets.
         *
         * @param existingSets list of all sets that have already been created
         * @return new Int id unique to all other existing sets
         */
        fun generateNewSetId(existingSets: List<ExerciseSet>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingSets.map { it.id }) }

            // Get the last (largest) ID in the sorted set, or start from 0 if the set is empty
            val largestId = sortedIds.lastOrNull() ?: 0

            // Return the next ID after the largest existing id
            return largestId + 1
        }

        /**
         * Generate IDs for existing metrics.
         *
         * @param existingMetrics list of all metrics that have already been created
         * @return new Int id unique to all other existing metrics
         */
        fun generateNewMetricId(existingMetrics: List<Metric>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingMetrics.map { it.id }) }

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
            val sets: MutableList<ExerciseSet> = mutableListOf()

            // Create an element in the list for each set
            for (setNumber in 0 until exerciseGoal.sets) {
                sets.add(ExerciseSet(
                    id = setNumber + 1
                ))
            }

            // Load the sets for the table
            setsAdapter.createSets(sets)
        }

        /**
         * Updates the visibility of a recycler view table based on the count of visible rows.
         *
         * @param adapter RecoveryOverviewAdapter to check the count of visible rows
         * @param tableLayout ConstraintLayout to update the visibility
         */
        fun updateRecyclerViewVisibility(adapter: RecoveryAdapter, tableLayout: ConstraintLayout) {
            if (adapter.getItemCount() <= 1) {
                tableLayout.visibility = View.GONE
            } else {
                tableLayout.visibility = View.VISIBLE
            }
        }

        /**
         * Updates the visibility of a recycler view overview table based on the count of visible rows.
         *
         * @param adapter RecoveryOverviewAdapter to check the count of visible rows
         * @param tableLayout ConstraintLayout to update the visibility
         */
        fun updateRecyclerViewOverviewVisibility(adapter: RecoveryOverviewAdapter, tableLayout: ConstraintLayout) {
            if (adapter.getVisibleItemCount() <= 1) {
                tableLayout.visibility = View.GONE
            } else {
                tableLayout.visibility = View.VISIBLE
            }
        }

        /**
         * Adds spacing between rows of a recycler view overview table.
         *
         * @param recyclerView RecyclerView table to add spacing
         * @param adapter RecoveryOverviewAdapter to check if rows are visible
         * @param bottomSpacing Int spacing to be added
         */
        fun addItemDecorationToRecyclerViewOverview(recyclerView: RecyclerView, adapter: RecoveryOverviewAdapter, bottomSpacing: Int = 12) {
            recyclerView.addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildAdapterPosition(view)

                        if (position == 0) {
                            outRect.bottom = bottomSpacing // Apply spacing
                        } else if (position > 0 && position < adapter.getItemCount() && adapter.isRowVisibleByPosition(position)) {
                            outRect.bottom = bottomSpacing // Apply spacing
                        } else {
                            outRect.bottom = 0 // No spacing for hidden rows
                        }
                    }
                }
            )
        }

        /**
         * Adds spacing between rows of a recycler view table.
         *
         * @param recyclerView RecyclerView table to add spacing
         * @param bottomSpacing Int spacing to be added
         */
        fun addItemDecorationToRecyclerView(recyclerView: RecyclerView, bottomSpacing: Int = 12) {
            recyclerView.addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.bottom = bottomSpacing
                    }
                }
            )
        }
    }
}
