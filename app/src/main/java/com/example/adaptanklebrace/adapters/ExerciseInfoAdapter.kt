package com.example.adaptanklebrace.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.fragments.ExerciseInfoFragment

class ExerciseInfoAdapter(
    fragmentActivity: FragmentActivity,
    private val exercises: List<ExerciseInfo>
) : FragmentStateAdapter(fragmentActivity) {

    // Returns the number of items in the ViewPager
    override fun getItemCount(): Int = exercises.size

    // Creates a new instance of ExerciseInfoFragment for each page
    override fun createFragment(position: Int): Fragment {
        return ExerciseInfoFragment.newInstance(exercises[position])
    }
}
