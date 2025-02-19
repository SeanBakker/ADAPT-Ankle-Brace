package com.example.adaptanklebrace

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.adaptanklebrace.adapters.ExerciseInfoAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.fragments.ConnectDeviceFragment

class StartMetricActivity : AppCompatActivity() {

    private lateinit var viewPagerInfo: ViewPager2
    private lateinit var metricInfoAdapter: ExerciseInfoAdapter
    private lateinit var connectToDeviceButton: Button

    private var chosenActivity: Class<*> = StartSetActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_metric)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.startMetricToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.start_metric)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Retrieve the passed Metric object with the intent
        @Suppress("DEPRECATION")
        val metric: Metric? = intent.getParcelableExtra(ExerciseInfo.METRIC_KEY)
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

        // Handle button to connect to the device
        connectToDeviceButton = findViewById(R.id.connectDeviceBtn)

        // Hide the visibility of the connect to device button when the metric is not recognized in the catalog
        if (metricInfo == null) {
            connectToDeviceButton.visibility = View.INVISIBLE
        } else {
            connectToDeviceButton.visibility = View.VISIBLE
        }

        connectToDeviceButton.setOnClickListener {
            metric?.let {
                // Choose target activity based on provided metric info
                if (metricInfo != null) {
                    if (metricInfo.name == ExerciseType.RANGE_OF_MOTION.exerciseName) {
                        chosenActivity = ROMMetricActivity::class.java
                    } else if (metricInfo.name == ExerciseType.GAIT_TEST.exerciseName) {
                        chosenActivity = GaitMetricActivity::class.java
                    }
                }

                val connectDeviceFragment = ConnectDeviceFragment(this, chosenActivity, Exercise(id = metric.id, name = metric.name, tension = 1))
                connectDeviceFragment.show(supportFragmentManager, "connect_device")
            }
        }
    }
}
