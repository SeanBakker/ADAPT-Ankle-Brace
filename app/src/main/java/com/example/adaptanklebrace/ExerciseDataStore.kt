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
        // Get unique key for storing exercises for this date
        val key = getExerciseKey(date)

        val json = gson.toJson(exercises)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getExercisesForDate(date: String): List<Exercise> {
        // Retrieve the JSON string using the unique key
        val key = getExerciseKey(date)
        val json = sharedPreferences.getString(key, null)

        return if (json != null) {
            val type = object : TypeToken<List<Exercise>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveDifficultyAndCommentsPairForDate(date: String, difficulty: Int, comments: String) {
        // Get unique key for storing difficulty & comments for this date
        val key = getDifficultyAndCommentsKey(date)

        // Combine the difficulty and comments into a JSON string
        val pair = Pair(difficulty, comments)
        val json = gson.toJson(pair)

        // Save it in SharedPreferences
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getDifficultyAndCommentsPairForDate(date: String): Pair<Int, String> {
        // Retrieve the JSON string using the unique key
        val key = getDifficultyAndCommentsKey(date)
        val json = sharedPreferences.getString(key, null)

        return if (json != null) {
            // Convert the JSON string back to a Pair object
            val type = object : TypeToken<Pair<Int, String>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Return a default Pair if no data is found
            Pair(0, "")
        }
    }

    private fun getExerciseKey(date: String): String {
        return "$date-exercises"
    }

    private fun getDifficultyAndCommentsKey(date: String): String {
        return "$date-difficulty-comments"
    }
}
