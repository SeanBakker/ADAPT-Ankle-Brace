package com.example.adaptanklebrace

import android.content.Context
import com.example.adaptanklebrace.data.Exercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExerciseDataStore(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("RecoveryPlanData", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveExercisesForDate(date: String, exercises: List<Exercise>) {
        val json = gson.toJson(exercises)
        sharedPreferences.edit().putString(date, json).apply()
    }

    fun getExercisesForDate(date: String): List<Exercise> {
        val json = sharedPreferences.getString(date, null)
        return if (json != null) {
            val type = object : TypeToken<List<Exercise>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
