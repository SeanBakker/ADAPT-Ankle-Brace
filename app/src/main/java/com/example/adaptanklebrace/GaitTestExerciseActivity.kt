package com.example.adaptanklebrace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseDataAdapter
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

class GaitTestExerciseActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var viewPagerData: ViewPager2
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter
    private lateinit var exerciseDataAdapter: ExerciseDataAdapter
    private lateinit var startSetButton: Button
    private lateinit var endSetButton: Button
    private lateinit var setsRecyclerView: RecyclerView
    private lateinit var finishButton: Button
    private lateinit var gaitStepsText: TextView
    private lateinit var gaitCadenceText: TextView
    private lateinit var gaitImpactForceText: TextView
    private lateinit var gaitSwingStanceRatioText: TextView

    private lateinit var bluetoothService: BluetoothService

    private lateinit var setsAdapter: ExerciseSetsTableRowAdapter
    private var sets: MutableList<Pair<Int, Int>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gait_test_exercise)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.gaitTestExerciseToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.gait_test_exercise)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            // Close bluetooth connection
            bluetoothService.disconnect()

            @Suppress("DEPRECATION")
            onBackPressed() // Go back to the previous activity
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        if (BluetoothService.instance == null) {
            GeneralUtil.showToast(this, layoutInflater, "Bluetooth service not available")
            finish() // Exit activity
            return
        }
        bluetoothService = BluetoothService.instance!!

        // Retrieve the passed Exercise object with the intent
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

        // Initialize the adapter
        setsAdapter = ExerciseSetsTableRowAdapter(sets)

        // Load sets data
        if (exercise != null) {
            ExerciseUtil.loadSetsData(setsAdapter, exercise)
        }

        // Setup recycler view table
        setsRecyclerView = findViewById(R.id.setsRecyclerView)
        setsRecyclerView.layoutManager = LinearLayoutManager(this)
        setsRecyclerView.adapter = setsAdapter

        // Initialize buttons & text views
        startSetButton = findViewById(R.id.startSetBtn)
        endSetButton = findViewById(R.id.endSetBtn)
        finishButton = findViewById(R.id.finishExerciseBtn)
        gaitStepsText = findViewById(R.id.gaitSteps)
        gaitCadenceText = findViewById(R.id.gaitCadence)
        gaitImpactForceText = findViewById(R.id.gaitImpactForce)
        gaitSwingStanceRatioText = findViewById(R.id.gaitSwingStanceRatio)

        // todo: add button listeners
        // todo: set textview data after a set is completed
    }
}
