package com.example.adaptanklebrace.utils

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryAdapter
import com.example.adaptanklebrace.adapters.RecoveryOverviewAdapter
import com.example.adaptanklebrace.data.Exercise
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
            val sets: MutableList<Pair<Int, Int>> = mutableListOf()

            // Create an element in the list for each set
            for (setNumber in 0 until exerciseGoal.sets) {
                sets.add(Pair(setNumber+1, 0))
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
    }
}
