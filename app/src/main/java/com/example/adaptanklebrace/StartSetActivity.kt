package com.example.adaptanklebrace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
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

class StartSetActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var viewPagerData: ViewPager2
    private lateinit var exerciseInfoAdapter: ExerciseInfoAdapter
    private lateinit var exerciseDataAdapter: ExerciseDataAdapter
    private lateinit var startSetButton: Button
    private lateinit var endSetButton: Button
    private lateinit var setsRecyclerView: RecyclerView
    private lateinit var finishButton: Button
    private lateinit var setProgressBar: ProgressBar
    private lateinit var setProgressLiveDataText: TextView
    private lateinit var setProgressMinText: TextView
    private lateinit var setProgressMaxText: TextView

    private lateinit var bluetoothService: BluetoothService
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var setsAdapter: ExerciseSetsTableRowAdapter
    private var sets: MutableList<Pair<Int, Int>> = mutableListOf()

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
            // Close bluetooth connection
            bluetoothService.disconnect()

            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        if (BluetoothService.instance == null) {
            ExerciseUtil.showToast(this, layoutInflater, "Bluetooth service not available")
            finish() // Exit activity
            return
        }
        bluetoothService = BluetoothService.instance!!

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

        // Initialize the progress bar and text views
        setProgressBar = findViewById(R.id.setProgressBar)
        setProgressLiveDataText = findViewById(R.id.setProgressLiveDataText)
        setProgressMinText = findViewById(R.id.setProgressMinText)
        setProgressMaxText = findViewById(R.id.setProgressMaxText)

        // todo: implement start/end set buttons
        // todo: once sets are complete, data must be saved in the Recovery Data table on clicking finish button
        // todo: on finishing an exercise, add simple dropdown to set the tension level manually

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
        bluetoothService._deviceLiveData.observe(this) { value ->
            updateProgress(value.toDouble())
            Log.i("ANGLE", "Angle data received: $value")
        }

        // Set test values
        setProgressBar.progress = 25 // Calculate this as a % of the max/min angles
        setProgressLiveDataText.text = "30°"

        // todo: set min/max values from test rep
        setProgressMinText.text = "10°"
        setProgressMaxText.text = "70°"
    }

    private fun updateProgress(anglesDegrees: Double) {
        setProgressBar.progress = anglesDegrees.toInt()
        setProgressLiveDataText.text = "$anglesDegrees°"
    }

    private val updateAngleTask = object : Runnable {
        override fun run() {
            // Read characteristic (device) data
            bluetoothService.readDeviceData()
            handler.postDelayed(this, 50)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateAngleTask)
        super.onDestroy()
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
