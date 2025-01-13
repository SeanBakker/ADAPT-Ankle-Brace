package com.example.adaptanklebrace

import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseDataAdapter
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.enums.ExerciseType

class StartSetActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var viewPagerData: ViewPager2
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter
    private lateinit var exerciseDataAdapter: ExerciseDataAdapter
    private lateinit var startSetButton: Button
    private lateinit var endSetButton: Button
    private lateinit var setsRecyclerView: RecyclerView
    private lateinit var finishButton: Button

    private lateinit var setsAdapter: ExerciseSetsTableRowAdapter
    private var sets: MutableList<Pair<Int, Int>> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_set)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.startSetToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.start_set)

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
        val exercise: Exercise? = intent.getParcelableExtra(ExerciseInfo.EXERCISE_KEY)
        var exerciseInfo: ExerciseInfo? = null

        // Retrieve the exercise info for the chosen exercise & set the exercise data adapter
        if (exercise != null) {
            exerciseInfo = ExerciseType.getExerciseInfoByName(exercise.name)
            exerciseDataAdapter = ExerciseDataAdapter(this, listOf(exercise))
        } else {
            exerciseDataAdapter = ExerciseDataAdapter(this, listOf(ExerciseType.getErrorExercise()))
        }

        // Set the exercise info adapter using the passed exercise or fallback to an error page
        exerciseInfoAdapter = if (exerciseInfo != null) {
            ExerciseInfoAdapter(this, listOf(exerciseInfo))
        } else {
            ExerciseInfoAdapter(this, listOf(ExerciseType.getErrorExerciseInfo()))
        }

        // Initialize and set up the ViewPagerInfo with an adapter
        viewPagerInfo = findViewById(R.id.viewPagerInfo)
        viewPagerInfo.adapter = exerciseInfoAdapter
        viewPagerInfo.isUserInputEnabled = false // Disable user input (swiping)

        // Initialize and set up the ViewPagerData with an adapter
        viewPagerData = findViewById(R.id.viewPagerData)
        viewPagerData.adapter = exerciseDataAdapter
        viewPagerData.isUserInputEnabled = false // Disable user input (swiping)

        // Initialize the adapter
        setsAdapter = ExerciseSetsTableRowAdapter(sets)

        // Load sets data
        if (exercise != null) {
            loadSetsData(exercise)
        }

        // Initialize and set up the RecyclerView
        setsRecyclerView = findViewById(R.id.setsRecyclerView)
        setsRecyclerView.layoutManager = LinearLayoutManager(this)
        setsRecyclerView.adapter = setsAdapter

        // todo: implement start/end set buttons
        // todo: once sets are complete, data must be saved in the Recovery Data table on clicking finish button
        // todo: implement progress bar for tracking rep data for basic exercises
    }

    // Load the sets data from the exercise goal
    private fun loadSetsData(exerciseGoal: Exercise) {
        val sets: MutableList<Pair<Int, Int>> = mutableListOf()

        // Create an element in the list for each set
        for (setNumber in 0 until exerciseGoal.sets) {
            sets.add(Pair(setNumber+1, 0))
        }

        // Load the sets for the table
        setsAdapter.createSets(sets)
    }
}
