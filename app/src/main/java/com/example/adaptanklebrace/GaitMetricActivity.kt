package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import com.example.adaptanklebrace.fragments.CountdownDialogFragment
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

class GaitMetricActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var metricInfoAdapter: ExerciseInfoAdapter
    private lateinit var startButton: Button
    private lateinit var finishButton: Button
    private lateinit var gaitStepsText: TextView
    private lateinit var gaitCadenceText: TextView
    private lateinit var gaitImpactForceText: TextView
    private lateinit var gaitSwingStanceRatioText: TextView

    private lateinit var bluetoothService: BluetoothService

    private var metric: Exercise? = null
    private val testIsComplete = -1f
    private var readFinalMetrics = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gait_metric)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.gaitTestMetricToolbar)
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
            onBackPressed() // Go back to the previous activity
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        if (BluetoothService.instance == null) {
            GeneralUtil.showToast(this, layoutInflater, getString(R.string.bluetoothServiceNotAvailableToast))
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

        // Initialize buttons & text views
        startButton = findViewById(R.id.startMetricBtn)
        finishButton = findViewById(R.id.finishMetricBtn)
        gaitStepsText = findViewById(R.id.gaitSteps)
        gaitCadenceText = findViewById(R.id.gaitCadence)
        gaitImpactForceText = findViewById(R.id.gaitImpactForce)
        gaitSwingStanceRatioText = findViewById(R.id.gaitSwingStanceRatio)

        // Hide the finish button until metric is complete
        finishButton.visibility = View.GONE

        // Observe data after the test completes
        bluetoothService.deviceLiveData.observe(this) { pair ->
            pair?.let {
                if (!readFinalMetrics) {
                    // Check for if the test is complete
                    if (it.first == testIsComplete && it.second == testIsComplete) {
                        // Update total ranges
                        Log.i("GAIT", "Test is complete! Flag sent: (${it.first},${it.second})")

                        // Write to device to send final reported metrics
                        bluetoothService.writeDeviceData("gait_steps")
                        Thread.sleep(1000) // Wait for device to load

                        // Read next values
                        readFinalMetrics = true
                        bluetoothService.readDeviceData()
                    }
                } else {
                    if (isFinalDataEmpty(gaitStepsText)) {
                        // Update steps recorded
                        Log.i("GAIT", "Steps count received: ${it.first}")
                        updateNumSteps(it.first.toInt())

                        // Write to device to send final reported metrics
                        bluetoothService.writeDeviceData("gait_cadence")
                        Thread.sleep(1000) // Wait for device to load

                        // Read next values
                        bluetoothService.readDeviceData()
                    } else if (isFinalDataEmpty(gaitCadenceText)) {
                        // Update cadence recorded
                        Log.i("GAIT", "Cadence received: ${it.first}")
                        updateCadence(it.first.toDouble())

                        // Write to device to send final reported metrics
                        Thread.sleep(1000) // Wait for device to load
                        bluetoothService.writeDeviceData("gait_force")
                        Thread.sleep(1000) // Wait for device to load

                        // Read next values
                        bluetoothService.readDeviceData()
                    } else if (isFinalDataEmpty(gaitImpactForceText)) {
                        // Update impact force recorded
                        Log.i("GAIT", "Impact force received: ${it.first}")
                        updateImpactForce(it.first.toDouble())

                        // Write to device to send final reported metrics
                        Thread.sleep(1000) // Wait for device to load
                        bluetoothService.writeDeviceData("gait_ratio")
                        Thread.sleep(1000) // Wait for device to load

                        // Read next values
                        bluetoothService.readDeviceData()
                    } else if (isFinalDataEmpty(gaitSwingStanceRatioText)) {
                        // Update swing stance ratio recorded
                        Log.i("GAIT", "Swing-stance ratio received: ${it.first}")
                        updateSwingStanceRatio(it.first.toDouble())

                        // Update visibility of buttons
                        finishButton.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Configure start button
        startButton.setOnClickListener {
            startButton.visibility = View.GONE
            val countdownDialog = CountdownDialogFragment(isGaitMetric = true) {
                // Send start_Gait flag to device to prepare metric data collection (of Gait Test)
                // This happens after the countdown finishes
                bluetoothService.writeDeviceData("start_Gait")
            }
            countdownDialog.show(supportFragmentManager, "countdown_dialog")
        }

        // Configure finish button
        finishButton.setOnClickListener {
            if (metric != null) {
                // Prompt user to input difficulty & comments
                val addDifficultyAndCommentsFragment = AddDifficultyAndCommentsFragment(this, isGaitTest = true)
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
     * Save Gait metric data to the Recovery Data table.
     *
     * @param difficulty the difficulty to save
     * @param comments the comments to save
     */
    fun saveGaitMetricData(difficulty: Int = 0, comments: String = "") {
        // Get existing metric data from the Recovery Data table
        val currentDate = GeneralUtil.getCurrentDate()
        val existingMetrics =
            ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getMetricsForDate(currentDate)

        // Save the metric data to the Recovery Data table
        val completedMetric = Metric(
            id = ExerciseUtil.generateNewMetricId(existingMetrics),
            name = metric!!.name,
            gaitNumSteps = gaitStepsText.text.toString().toIntOrNull() ?: 0,
            gaitCadence = gaitCadenceText.text.toString().toDoubleOrNull() ?: 0.0,
            gaitImpactForce = gaitImpactForceText.text.toString().toDoubleOrNull() ?: 0.0,
            gaitSwingStanceRatio = gaitSwingStanceRatioText.text.toString().toDoubleOrNull() ?: 0.0,
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
    private fun updateNumSteps(steps: Int) {
        gaitStepsText.text = String.format("%d", steps)
    }

    @SuppressLint("DefaultLocale")
    private fun updateCadence(cadence: Double) {
        gaitCadenceText.text = String.format("%.1f", cadence)
    }

    @SuppressLint("DefaultLocale")
    private fun updateImpactForce(force: Double) {
        gaitImpactForceText.text = String.format("%.1f", force)
    }

    @SuppressLint("DefaultLocale")
    private fun updateSwingStanceRatio(ratio: Double) {
        gaitSwingStanceRatioText.text = String.format("%.1f", ratio)
    }

    private fun isFinalDataEmpty(view: TextView): Boolean {
        return view.text == getString(R.string.noData)
    }

    override fun onDestroy() {
        // Close bluetooth connection
        bluetoothService.disconnect()

        super.onDestroy()
    }
}
