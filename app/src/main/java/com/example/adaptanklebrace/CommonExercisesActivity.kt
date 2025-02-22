package com.example.adaptanklebrace

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.enums.ExerciseType

class CommonExercisesActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var quickLinksLayout: LinearLayout
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_exercises)

        // Initialize viewPager and quickLinksLayout
        viewPager = findViewById(R.id.viewPager)
        quickLinksLayout = findViewById(R.id.quick_links_layout)

        // Set up the ViewPager with an adapter
        exerciseInfoAdapter = ExerciseInfoAdapter(this, ExerciseType.getAllExercises())
        viewPager.adapter = exerciseInfoAdapter
    }

    // This method is triggered when a quick link button is clicked
    fun onQuickLinkClick(view: View) {
        when (view.id) {
            R.id.btn_exercise_1 -> viewPager.currentItem = 0
            R.id.btn_exercise_2 -> viewPager.currentItem = 1
            R.id.btn_exercise_3 -> viewPager.currentItem = 2
            R.id.btn_exercise_4 -> viewPager.currentItem = 3
            R.id.btn_exercise_5 -> viewPager.currentItem = 4
            R.id.btn_exercise_6 -> viewPager.currentItem = 5
        }
    }

    // This method is triggered when a change page button is clicked
    fun onChangePageClick(view: View) {
        when (view.id) {
            R.id.prevExerciseBtn -> {
                if (viewPager.currentItem == 0) {
                    viewPager.currentItem = 0
                } else {
                    viewPager.currentItem--
                }
            }
            R.id.nextExerciseBtn -> {
                val exerciseListSize = ExerciseType.getSize()
                if (viewPager.currentItem == exerciseListSize - 1) {
                    viewPager.currentItem = exerciseListSize
                } else {
                    viewPager.currentItem++
                }
            }
        }
    }
}
