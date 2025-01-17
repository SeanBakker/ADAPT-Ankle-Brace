package com.example.adaptanklebrace

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseDataAdapter
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.fragments.ConnectDeviceFragment

class StartExerciseActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var viewPagerData: ViewPager2
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter
    private lateinit var exerciseDataAdapter: ExerciseDataAdapter
    private lateinit var connectToDeviceButton: Button

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
            ExerciseInfoAdapter(this, listOf(ExerciseType.getErrorExerciseInfo(exercise?.name ?: ExerciseType.ERROR.exerciseName)))
        }

        // Initialize and set up the ViewPagerInfo with an adapter
        viewPagerInfo = findViewById(R.id.viewPagerInfo)
        viewPagerInfo.adapter = exerciseInfoAdapter
        viewPagerInfo.isUserInputEnabled = false // Disable user input (swiping)

        // Initialize and set up the ViewPagerData with an adapter
        viewPagerData = findViewById(R.id.viewPagerData)
        viewPagerData.adapter = exerciseDataAdapter
        viewPagerData.isUserInputEnabled = false // Disable user input (swiping)

        // Handle button to connect to the device
        connectToDeviceButton = findViewById(R.id.connectDeviceBtn)

        // Hide the visibility of the connect to device button when the exercise is not recognized in the catalog
        if (exerciseInfo == null) {
            connectToDeviceButton.visibility = View.INVISIBLE
        } else {
            connectToDeviceButton.visibility = View.VISIBLE
        }

        connectToDeviceButton.setOnClickListener {
            exercise?.let {
                // todo: update target activity depending on exercise type
                val connectDeviceFragment = ConnectDeviceFragment(this, StartSetActivity::class.java, it)
                connectDeviceFragment.show(supportFragmentManager, "connect_device")
            }
        }
    }
}
