package com.example.adaptanklebrace.adapters

import com.example.adaptanklebrace.data.Exercise

interface RecoveryPlanAdapter {
    fun setExercises(newExercises: List<Exercise>)
    fun getExercises(): List<Exercise>
    fun addExerciseRow(exercise: Exercise)
    fun notifyItemChangedAndRefresh(position: Int)
    fun refreshTable()
}