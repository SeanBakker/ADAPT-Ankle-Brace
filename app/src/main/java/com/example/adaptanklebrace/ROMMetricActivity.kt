package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.RecoveryDataActivity.Companion.RECOVERY_DATA_PREFERENCE
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.fragments.AddDifficultyAndCommentsFragment
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

class ROMMetricActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var metricInfoAdapter: ExerciseInfoAdapter
    private lateinit var flexionProgressBar: ProgressBar
    private lateinit var inversionProgressBar: ProgressBar
    private lateinit var flexionPercentageText: TextView
    private lateinit var inversionPercentageText: TextView
    private lateinit var flexionTotalROM: TextView
    private lateinit var inversionTotalROM: TextView
    private lateinit var startButton: Button
    private lateinit var finishButton: Button
    private lateinit var bluetoothService: BluetoothService
    private val handler = Handler(Looper.getMainLooper())

    private var metric: Exercise? = null
    private val testIsComplete = -1f
    private var readTotalROMAngles = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rom_metric)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.romMetricToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.rom_metric)

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

        // Retrieve the passed metric object with the intent
        @Suppress("DEPRECATION")
        metric = intent.getParcelableExtra(ExerciseInfo.METRIC_KEY)
        var metricInfo: ExerciseInfo? = null

        // Retrieve the metric info for the chosen metric
        if (metric != null) {
            metricInfo = ExerciseType.getExerciseInfoByName(metric!!.name)
        }

        // Set the metric info adapter using the passed metric or fallback to an error page
        metricInfoAdapter = if (metricInfo != null) {
            ExerciseInfoAdapter(this, listOf(metricInfo))
        } else {
            ExerciseInfoAdapter(this, listOf(ExerciseType.getErrorExerciseInfo(metric?.name ?: ExerciseType.ERROR.exerciseName)))
        }

        // Initialize and set up the ViewPagerInfo with an adapter
        viewPagerInfo = findViewById(R.id.viewPagerInfo)
        viewPagerInfo.adapter = metricInfoAdapter
        viewPagerInfo.isUserInputEnabled = false // Disable user input (swiping)

        // Initialize progress bar, buttons & text views
        startButton = findViewById(R.id.startROMBtn)
        finishButton = findViewById(R.id.finishMetricBtn)
        flexionProgressBar = findViewById(R.id.flexionProgress)
        inversionProgressBar = findViewById(R.id.inversionProgress)
        flexionPercentageText = findViewById(R.id.flexionPercentageText)
        inversionPercentageText = findViewById(R.id.inversionPercentageText)
        flexionTotalROM = findViewById(R.id.flexionROMTotal)
        inversionTotalROM = findViewById(R.id.inversionROMTotal)

        // Hide the finish button until metric is complete
        finishButton.visibility = View.GONE

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
        bluetoothService.deviceLiveData.observe(this) { pair ->
            pair?.let {
                // Check for if the test is complete
                if (it.first == testIsComplete && it.second == testIsComplete) {
                    // Update total ranges
                    Log.i("ROM", "Test is complete! Flag sent: (${it.first},${it.second})")
                    Thread.sleep(100) // Wait for device to load

                    // Read next values
                    readTotalROMAngles = true
                    bluetoothService.readDeviceData()
                    handler.removeCallbacks(updateAngleTask)
                } else if (readTotalROMAngles) {
                    // Update total ROM ranges
                    updateTotalROM(Pair(it.first.toDouble(), it.second.toDouble()))

                    // Update visibility of buttons
                    finishButton.visibility = View.VISIBLE
                    startButton.visibility = View.GONE
                } else {
                    // Update live data
                    updateProgress(Pair(it.first.toDouble(), it.second.toDouble()))
                    Log.i("ROM", "Angle data received: (${it.first},${it.second})")
                }
            }
        }

        // Configure start set button
        startButton.setOnClickListener {
            // Send start_ROM flag to device to prepare metric data collection (of ROM Test)
            bluetoothService.writeDeviceData("start_ROM")
        }

        // Configure finish button
        finishButton.setOnClickListener {
            if (metric != null) {
                // Prompt user to input difficulty & comments
                val addDifficultyAndCommentsFragment = AddDifficultyAndCommentsFragment(this, isROMTest = true)
                addDifficultyAndCommentsFragment.show(
                    supportFragmentManager,
                    "add_difficulty_and_comments"
                )
            } else {
                // Redirect the user back to the Recovery Data page
                startActivity(Intent(this, RecoveryDataActivity::class.java))
            }
        }
    }

    /**
     * Save ROM metric data to the Recovery Data table.
     *
     * @param difficulty the difficulty to save
     * @param comments the comments to save
     */
    fun saveROMMetricData(difficulty: Int = 0, comments: String = "") {
        // Get existing metric data from the Recovery Data table
        val currentDate = GeneralUtil.getCurrentDate()
        val existingMetrics =
            ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getMetricsForDate(currentDate)

        // Save the metric data to the Recovery Data table
        val completedMetric = Metric(
            id = ExerciseUtil.generateNewMetricId(existingMetrics),
            name = metric!!.name,
            romPlantarDorsiflexionRange = flexionTotalROM.text.toString().toDoubleOrNull() ?: 0.0,
            romInversionEversionRange = inversionTotalROM.text.toString().toDoubleOrNull() ?: 0.0,
            difficulty = difficulty,
            comments = comments
        )
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveMetricsForDate(
            currentDate,
            existingMetrics + completedMetric
        )

        // Redirect the user back to the Recovery Data page
        startActivity(Intent(this, RecoveryDataActivity::class.java))
        finish() // Finish activity (prevent running in background)
    }

    @SuppressLint("DefaultLocale")
    private fun updateProgress(anglesDegrees: Pair<Double, Double>) {
        val (flexionAngle, inversionAngle) = anglesDegrees
        flexionProgressBar.progress = flexionAngle.toInt()
        flexionPercentageText.text = String.format("%.1f°", flexionAngle)

        inversionProgressBar.progress = inversionAngle.toInt()
        inversionPercentageText.text = String.format("%.1f°", inversionAngle)
    }

    @SuppressLint("DefaultLocale")
    private fun updateTotalROM(anglesDegrees: Pair<Double, Double>) {
        val (flexionTotal, inversionTotal) = anglesDegrees
        flexionTotalROM.text = String.format("%.1f°", flexionTotal)
        inversionTotalROM.text = String.format("%.1f°", inversionTotal)
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
