package com.example.adaptanklebrace

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
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

    companion object {
        @BindingAdapter("imageFromResourceId")
        @JvmStatic
        fun setImageFromResourceId(view: ImageView, imageId: Int?) {
            imageId?.let {
                //todo: add exercise images
                val drawableResId = when (it) {
                    1 -> R.drawable.baseline_directions_walk_24 // exercise #1
                    2 -> R.drawable.baseline_directions_run_24 // exercise #2
                    3 -> R.drawable.baseline_healing_24 // exercise #3
                    4 -> R.drawable.baseline_healing_24 // exercise #4
                    5 -> R.drawable.baseline_healing_24 // exercise #5
                    6 -> R.drawable.baseline_healing_24 // exercise #6
                    7 -> R.drawable.baseline_healing_24 // exercise #7
                    else -> R.drawable.baseline_error_24 // error
                }
                view.setImageResource(drawableResId)
            }
        }
    }
}
