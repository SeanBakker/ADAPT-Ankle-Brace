package com.example.adaptanklebrace

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.enums.ExerciseType

class CommonExercisesActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var quickLinksLayout: LinearLayout
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_exercises)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.commonExerciseToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.common_exercises)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

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
            R.id.btn_exercise_7 -> viewPager.currentItem = 6
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
