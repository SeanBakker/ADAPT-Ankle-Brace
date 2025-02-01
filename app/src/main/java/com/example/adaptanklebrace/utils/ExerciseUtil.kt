package com.example.adaptanklebrace.utils

import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.data.Exercise

class ExerciseUtil {
    companion object {
        /**
         * Generate IDs for existing exercises.
         *
         * @param existingExercises list of all exercises that have already been created
         * @return new Int id unique to all other existing exercises
         */
        fun generateNewId(existingExercises: List<Exercise>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingExercises.map { it.id }) }

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
    }
}
