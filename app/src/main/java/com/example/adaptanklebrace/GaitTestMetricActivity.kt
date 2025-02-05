package com.example.adaptanklebrace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.GeneralUtil

class GaitTestMetricActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var metricInfoAdapter: ExerciseInfoAdapter
    private lateinit var finishButton: Button
    private lateinit var gaitStepsText: TextView
    private lateinit var gaitCadenceText: TextView
    private lateinit var gaitImpactForceText: TextView
    private lateinit var gaitSwingStanceRatioText: TextView

    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gait_test_metric)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.gaitTestMetricToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.gait_test_metric)

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
        val metric: Exercise? = intent.getParcelableExtra(ExerciseInfo.METRIC_KEY)
        var metricInfo: ExerciseInfo? = null

        // Retrieve the metric info for the chosen metric
        if (metric != null) {
            metricInfo = ExerciseType.getExerciseInfoByName(metric.name)
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
        finishButton = findViewById(R.id.finishMetricBtn)
        gaitStepsText = findViewById(R.id.gaitSteps)
        gaitCadenceText = findViewById(R.id.gaitCadence)
        gaitImpactForceText = findViewById(R.id.gaitImpactForce)
        gaitSwingStanceRatioText = findViewById(R.id.gaitSwingStanceRatio)

        // todo: add button listeners
        // todo: set textview data after a set is completed
    }
}
