package com.example.adaptanklebrace.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R

class ExerciseItemAdapter(
    private val exerciseList: List<String>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<ExerciseItemAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Hold the TextView for the exercise name
        val exerciseNameTextView: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        // Inflate the row item view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recovery_data_row_item, parent, false)

        // Get the exercise name TextView from the inflated layout
        val exerciseNameTextView: TextView = view.findViewById(R.id.exerciseName)

        return ExerciseViewHolder(exerciseNameTextView)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        // Set the exercise name in the TextView
        holder.exerciseNameTextView.text = exerciseList[position]
    }

    override fun getItemCount() = exerciseList.size
}
