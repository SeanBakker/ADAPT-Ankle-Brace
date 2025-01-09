package com.example.adaptanklebrace

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExercisePagesAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType

class StartExerciseActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var exercisePagesAdapter: ExercisePagesAdapter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_exercise)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.startExerciseToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.start_exercise)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Retrieve the passed Exercise object with the intent
        @Suppress("DEPRECATION")
        val exercise: Exercise? = intent.getParcelableExtra(Exercise.EXERCISE_KEY)

        // Set the adapter using the passed exercise or fallback to an error page
        exercisePagesAdapter = if (exercise != null) {
            ExercisePagesAdapter(this, listOf(exercise))
        } else {
            ExercisePagesAdapter(this, listOf(ExerciseType.getExerciseError()))
        }

        // Initialize and set up the ViewPager with an adapter
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = exercisePagesAdapter

        //todo: implement connect to device button
    }
}
