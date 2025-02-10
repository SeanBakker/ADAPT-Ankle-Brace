package com.example.adaptanklebrace

import android.annotation.SuppressLint
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
import com.example.adaptanklebrace.RecoveryDataActivity.Companion.RECOVERY_DATA_PREFERENCE
import com.example.adaptanklebrace.adapters.ExerciseDataAdapter
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.adapters.ExerciseSetsTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.data.ExerciseSet
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

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

    private var isFirstRun = true
    private var minAngle: Int = 0
    private var maxAngle: Int = 360

    private lateinit var setsAdapter: ExerciseSetsTableRowAdapter
    private var sets: MutableList<ExerciseSet> = mutableListOf()

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
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

        // Initialize and set up the RecyclerView
        setsRecyclerView = findViewById(R.id.setsRecyclerView)
        setsRecyclerView.layoutManager = LinearLayoutManager(this)
        setsRecyclerView.adapter = setsAdapter

        // Initialize progress bar, buttons & text views
        startSetButton = findViewById(R.id.startSetBtn)
        endSetButton = findViewById(R.id.endSetBtn)
        finishButton = findViewById(R.id.finishExerciseBtn)
        setProgressBar = findViewById(R.id.setProgressBar)
        setProgressLiveDataText = findViewById(R.id.setProgressLiveDataText)
        setProgressMinText = findViewById(R.id.setProgressMinText)
        setProgressMaxText = findViewById(R.id.setProgressMaxText)

        // todo: implement start/end set buttons
        // todo: on finishing an exercise, add simple dropdown to set the tension level manually

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
        bluetoothService.deviceLiveData.observe(this) { pair ->
            pair?.let {
                if (isFirstRun) {
                    Log.i("ANGLE", "Min angle received: ${it.first}")
                    Log.i("ANGLE", "Max angle received: ${it.second}")
                    updateMinMaxAngles(it.first, it.second)
                    isFirstRun = false // Mark that the first run is complete
                } else {
                    // This runs after the first time
                    updateProgress(it.first.toDouble())
                    Log.i("ANGLE", "Angle data received: ${it.first}")
                }
            }
        }

        // Configure start set button
        startSetButton.setOnClickListener {
            // Send start flag to device to prepare exercise data collection (of sets)
            bluetoothService.writeDeviceData("start")
        }

        // Configure end set button
        endSetButton.setOnClickListener {
            // todo: implement this button to fill in table row with data from device
        }

        // Configure finish button
        finishButton.setOnClickListener {
            if (exercise != null) {
                // Send finish flag to device to end the exercise
                bluetoothService.writeDeviceData("finish")

                // Save the exercise data to the Recovery Data table
                val currentDate = GeneralUtil.getCurrentDate()
                val existingExercises =
                    ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getExercisesForDate(
                        currentDate
                    )

                // Calculate sets completed as all >0 reps rows in the table
                val setsCompleted = setsAdapter.getNonZeroRowsCount()

                // Calculate reps completed as an average of all >0 reps in the table
                val repsCompleted = setsAdapter.getAverageReps()

                // todo: ask for difficulty rating & comments before saving data
                val completedExercise = Exercise(
                    id = ExerciseUtil.generateNewExerciseId(existingExercises),
                    name = exercise.name,
                    sets = setsCompleted,
                    reps = repsCompleted,
                    hold = exercise.hold,
                    tension = exercise.tension, // todo: update with actual tension from device
                )
                ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveExercisesForDate(
                    currentDate,
                    existingExercises + completedExercise
                )
            }

            // Redirect the user back to the Recovery Data page
            startActivity(Intent(this, RecoveryDataActivity::class.java))
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateProgress(anglesDegrees: Double) {
        // Calculate progress as a percentage of the angle range
        val progressPercentage = ((anglesDegrees - minAngle) / (maxAngle - minAngle)) * 100

        // Update progress bar
        setProgressBar.progress = progressPercentage.toInt()
        setProgressLiveDataText.text = "$anglesDegrees°"
    }

    @SuppressLint("SetTextI18n")
    private fun updateMinMaxAngles(minAngleValue: Int, maxAngleValue: Int) {
        minAngle = minAngleValue
        maxAngle = maxAngleValue
        setProgressMinText.text = "$minAngleValue°"
        setProgressMaxText.text = "$maxAngleValue°"
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
}
