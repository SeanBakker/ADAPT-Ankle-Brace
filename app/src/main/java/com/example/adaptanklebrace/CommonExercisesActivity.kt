package com.example.adaptanklebrace

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.data.Exercise

class CommonExercisesActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var quickLinksLayout: LinearLayout
    private lateinit var exercisePagesAdapter: ExercisePagesAdapter

    // Example list of exercises
    private val EXERCISE_LIST = listOf(
        Exercise("Ankle Stretch", "Description of ankle stretch", "Steps to do it", 1),
        Exercise("Heel Raise", "Description of heel raise", "Steps to do it", 2),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.common_exercises)

        // Initialize viewPager and quickLinksLayout
        viewPager = findViewById(R.id.viewPager)
        quickLinksLayout = findViewById(R.id.quick_links_layout)

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

        // Set up the ViewPager with an adapter
        exercisePagesAdapter = ExercisePagesAdapter(this, EXERCISE_LIST)
        viewPager.adapter = exercisePagesAdapter
    }

    // This method is triggered when a quick link button is clicked
    fun onQuickLinkClick(view: View) {
        when (view.id) {
            R.id.btn_exercise_1 -> viewPager.currentItem = 0
            R.id.btn_exercise_2 -> viewPager.currentItem = 1
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
                val exerciseListSize = EXERCISE_LIST.size
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
                val drawableResId = when (it) {
                    1 -> R.drawable.baseline_directions_walk_24 // exercise #1
                    2 -> R.drawable.baseline_directions_run_24 // exercise #2
                    else -> R.drawable.baseline_error_24 // error
                }
                view.setImageResource(drawableResId)
            }
        }
    }
}
