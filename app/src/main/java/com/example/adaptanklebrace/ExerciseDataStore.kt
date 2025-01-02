package com.example.adaptanklebrace

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.adaptanklebrace.adapters.LocalTimeAdapter
import com.example.adaptanklebrace.data.Exercise
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.Q)
class ExerciseDataStore(context: Context, preferenceName: String) {
    private val sharedPreferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())  // Register the LocalTime adapter
        .create()

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
