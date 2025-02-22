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
import com.example.adaptanklebrace.fragments.AddDifficultyAndCommentsFragment
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import kotlin.math.floor

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

    private var exercise: Exercise? = null
    private var isFirstRun = true
    private var isSetCompleted = false
    private var isLiveData = false
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
        supportActionBar?.title = ""

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
        exercise = intent.getParcelableExtra(ExerciseInfo.EXERCISE_KEY)
        var exerciseInfo: ExerciseInfo? = null

        // Retrieve the exercise info for the chosen exercise & set the exercise data adapter
        if (exercise != null) {
            exerciseInfo = ExerciseType.getExerciseInfoByName(exercise!!.name)
            exerciseDataAdapter = ExerciseDataAdapter(this, listOf(exercise!!))
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
            ExerciseUtil.loadSetsData(setsAdapter, exercise!!)
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

        // Observe data from the device
        bluetoothService.readDeviceData() // Read initial min/max values from test rep
        bluetoothService.deviceLiveData.observe(this) { pair ->
            pair?.let {
                if (isFirstRun) {
                    isFirstRun = false // Mark that the first run is complete
                    Log.i("SET", "Min angle received: ${it.first}")
                    Log.i("SET", "Max angle received: ${it.second}")
                    updateMinMaxAngles(it.first.toInt(), it.second.toInt())
                    updateProgress(0.0)
                } else if (isSetCompleted) {
                    isSetCompleted = false
                    // Updates rep count when set is completed
                    Log.i("SET", "Rep count received: ${it.first}")
                    updateRepCount(it.first.toInt())
                } else if (isLiveData) {
                    // Updates live data
                    updateProgress(it.first.toDouble())
                    Log.i("SET", "Angle data received: ${it.first}")
                }
            }
        }

        // Configure start set button
        startSetButton.setOnClickListener {
            isLiveData = true

            // Send start flag to device to prepare exercise data collection (of sets)
            val startExerciseFlag = when (exercise?.name) {
                ExerciseType.PLANTAR_FLEXION.exerciseName -> "start_plantar"
                ExerciseType.DORSIFLEXION.exerciseName -> "start_dorsiflexion"
                ExerciseType.INVERSION.exerciseName -> "start_inversion"
                ExerciseType.EVERSION.exerciseName -> "start_eversion"
                else -> "error"
            }
            bluetoothService.writeDeviceData(startExerciseFlag)

            // Periodically read angle from device
            handler.post(updateAngleTask)
        }

        // Configure end set button
        endSetButton.setOnClickListener {
            isLiveData = false

            // Stop reading periodically from device
            handler.removeCallbacks(updateAngleTask)
            Thread.sleep(1000) // Wait for device to load

            // Send end flag to device to end exercise live data collection
            bluetoothService.writeDeviceData("end_set")
            Thread.sleep(1000) // Wait for device to load

            // Read the count of reps determined for that set
            isSetCompleted = true
            bluetoothService.readDeviceData()
        }

        // Configure finish button
        finishButton.setOnClickListener {
            if (exercise != null) {
                // Prevent saving of data when no reps are completed
                if (setsAdapter.getAverageReps() > 0) {
                    // Prompt user to input difficulty & comments
                    val addDifficultyAndCommentsFragment = AddDifficultyAndCommentsFragment(this, expectedTension = exercise!!.tension)
                    addDifficultyAndCommentsFragment.show(
                        supportFragmentManager,
                        "add_difficulty_and_comments"
                    )
                } else {
                    // Warn user of no data to save
                    GeneralUtil.showToast(this, layoutInflater, "No sets and/or reps submitted. Please complete 1 rep before saving data.")
                }
            } else {
                // Redirect the user back to the Recovery Data page
                startActivity(Intent(this, RecoveryDataActivity::class.java))
            }
        }
    }

    /**
     * Save set data to the Recovery Data table.
     *
     * @param tension the tension to save
     * @param difficulty the difficulty to save
     * @param comments the comments to save
     */
    fun saveSetData(tension: Int = exercise!!.tension, difficulty: Int = 0, comments: String = "") {
        // Get existing exercise data from the Recovery Data table
        val currentDate = GeneralUtil.getCurrentDate()
        val existingExercises =
            ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getExercisesForDate(
                currentDate
            )

        // Calculate sets completed as all >0 reps rows in the table
        val setsCompleted = setsAdapter.getNonZeroRowsCount()

        // Calculate reps completed as an average of all >0 reps in the table
        val repsCompleted = setsAdapter.getAverageReps()

        // Sets and reps must be >0 to save data
        if (setsCompleted > 0 && repsCompleted > 0) {
            // Save the exercise data to the Recovery Data table
            val completedExercise = Exercise(
                id = ExerciseUtil.generateNewExerciseId(existingExercises),
                name = exercise!!.name,
                sets = setsCompleted,
                reps = repsCompleted,
                hold = exercise!!.hold,
                tension = tension,
                difficulty = difficulty,
                comments = comments
            )
            ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveExercisesForDate(
                currentDate,
                existingExercises + completedExercise
            )

            // Redirect the user back to the Recovery Data page
            startActivity(Intent(this, RecoveryDataActivity::class.java))
            finish() // Finish activity (prevent running in background)
        } else {
            // Warn user of no data to save
            GeneralUtil.showToast(this, layoutInflater, "No sets and/or reps submitted. Please complete 1 rep before saving data.")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateProgress(anglesDegrees: Double) {
        // Calculate progress as a percentage of the angle range
        val progressPercentage = ((anglesDegrees - minAngle) / (maxAngle - minAngle)) * 100

        // Update progress bar
        setProgressBar.progress = floor(progressPercentage).toInt()
        setProgressLiveDataText.text = String.format("%.1f°", anglesDegrees)
    }

    @SuppressLint("DefaultLocale")
    private fun updateMinMaxAngles(minAngleValue: Int, maxAngleValue: Int) {
        minAngle = minAngleValue
        maxAngle = maxAngleValue
        setProgressMinText.text = String.format("%d°", minAngleValue)
        setProgressMaxText.text = String.format("%d°", maxAngleValue)
    }

    /**
     * Updates the rep count of the corresponding set row in the table.
     * If all rows are non-zero, a new row is added to the table.
     *
     * @param newRepCount reps counted from the device to store
     */
    private fun updateRepCount(newRepCount: Int) {
        val exerciseSet = setsAdapter.getNextSetWithZeroReps()

        if (exerciseSet != null) {
            // Row exists, update the reps count
            exerciseSet.reps = newRepCount
            setsAdapter.notifyItemChanged(exerciseSet.id)
        } else {
            // All rows are non-zero, create new row
            setsAdapter.addSetRow(
                ExerciseSet(
                    id = ExerciseUtil.generateNewSetId(sets),
                    reps = newRepCount
                )
            )
        }
    }

    private val updateAngleTask = object : Runnable {
        override fun run() {
            // Read characteristic (device) data
            bluetoothService.readDeviceData()
            handler.postDelayed(this, 50)
        }
    }

    override fun onDestroy() {
        // Close bluetooth connection
        bluetoothService.disconnect()

        // Remove callbacks
        handler.removeCallbacks(updateAngleTask)
        super.onDestroy()
    }
}
