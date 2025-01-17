package com.example.adaptanklebrace.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise

class ExerciseUtil {
    companion object {
        fun generateNewId(existingExercises: List<Exercise>): Int {
            // Map the IDs to a sorted set
            val sortedIds = sortedSetOf<Int>().apply { addAll(existingExercises.map { it.id }) }

            // Get the last (largest) ID in the sorted set, or start from 0 if the set is empty
            val largestId = sortedIds.lastOrNull() ?: 0

            // Return the next ID after the largest existing id
            return largestId + 1
        }

        @SuppressLint("InflateParams")
        fun showToast(context: Context, layoutInflater: LayoutInflater, message: String) {
            // Retrieve the custom toast background
            val inflater = layoutInflater
            val customView = inflater.inflate(R.layout.custom_toast, null)
            val customToast = customView.findViewById<TextView>(R.id.toast_text)
            customToast.text = message

            val toast = Toast(context)
            toast.duration = Toast.LENGTH_SHORT
            toast.setGravity(Gravity.TOP, 0, 10)
            toast.view = customView
            toast.show()
        }
    }
}