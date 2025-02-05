package com.example.adaptanklebrace.adapters

import com.example.adaptanklebrace.data.Exercise

/**
 * Adapter for specific functionality of exercises.
 */
interface RecoveryExerciseAdapter: RecoveryAdapter {
    fun setExercises(newExercises: List<Exercise>)
    fun getExercises(): List<Exercise>
    fun addExerciseRow(exercise: Exercise)
}
