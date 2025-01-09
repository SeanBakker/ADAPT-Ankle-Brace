package com.example.adaptanklebrace.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.fragments.ExerciseDataFragment

class ExerciseDataAdapter(
    fragmentActivity: FragmentActivity,
    private val exercises: List<Exercise>
) : FragmentStateAdapter(fragmentActivity) {

    // Returns the number of items in the ViewPager
    override fun getItemCount(): Int = exercises.size

    // Creates a new instance of ExerciseDataFragment for each page
    override fun createFragment(position: Int): Fragment {
        return ExerciseDataFragment.newInstance(exercises[position])
    }
}
