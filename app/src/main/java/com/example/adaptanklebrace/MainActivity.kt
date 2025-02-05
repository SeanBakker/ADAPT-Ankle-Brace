package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewExerciseTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewMetricTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import com.example.adaptanklebrace.utils.SharedPreferencesUtil
import com.google.android.material.navigation.NavigationView
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity(), RecoveryPlanOverviewExerciseTableRowAdapter.MainActivityCallback,
    RecoveryPlanOverviewMetricTableRowAdapter.MainActivityCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var weeklyProgressBar: ProgressBar
    private lateinit var weeklyProgressText: TextView
    private lateinit var editGoalsBtn: Button
    private lateinit var goalsCompletedText: TextView
    private lateinit var noGoalsSetText: TextView
    private lateinit var setGoalsBtn: Button

    // Exercise table variables
    private lateinit var exerciseTableLayout: ConstraintLayout
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exerciseAdapter: RecoveryPlanOverviewExerciseTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    // Metric table variables
    private lateinit var metricTableLayout: ConstraintLayout
    private lateinit var metricRecyclerView: RecyclerView
    private lateinit var metricAdapter: RecoveryPlanOverviewMetricTableRowAdapter
    private var metrics: MutableList<Metric> = mutableListOf()

    // Initialize counter for updating the two recycler views
    private var recyclerViewUpdatedCounter = AtomicInteger(2)

    private var settingsActivity = SettingsActivity()
    private var recoveryPlanActivity = RecoveryPlanActivity()
    private lateinit var bluetoothService: BluetoothService
    private var isBluetoothServiceBound = false
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1002

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothService.LocalBinder).getService()
            isBluetoothServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBluetoothServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the app theme
        sharedPreferences = getSharedPreferences(SettingsActivity.SETTINGS_PREFERENCE, MODE_PRIVATE)
        val isNightMode = SharedPreferencesUtil.getPreference(sharedPreferences, SettingsActivity.NIGHT_MODE_KEY, false)
        settingsActivity.changeAppTheme(isNightMode)
        setContentView(R.layout.activity_main)

        // Set up Toolbar
        val toolbar: Toolbar = findViewById(R.id.homeToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24) // Icon for sidebar

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)

        // Initialize NavigationView
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_common_exercises -> startActivity(Intent(this, CommonExercisesActivity::class.java))
                R.id.nav_recovery_plan -> startActivity(Intent(this, RecoveryPlanActivity::class.java))
                R.id.nav_recovery_data -> startActivity(Intent(this, RecoveryDataActivity::class.java))
                R.id.nav_notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
            }
            drawerLayout.closeDrawers() // Close the sidebar
            true
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Initialize the adapters and pass the activity as a callback
        exerciseAdapter = RecoveryPlanOverviewExerciseTableRowAdapter(exercises, this)
        metricAdapter = RecoveryPlanOverviewMetricTableRowAdapter(metrics, this)

        // Set up RecyclerView for exercise table
        exerciseTableLayout = findViewById(R.id.exerciseTableLayout)
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter

        // Set up RecyclerView for metric table
        metricTableLayout = findViewById(R.id.metricTableLayout)
        metricRecyclerView = findViewById(R.id.metricRecyclerView)
        metricRecyclerView.layoutManager = LinearLayoutManager(this)
        metricRecyclerView.adapter = metricAdapter

        // Load data for current week on activity start
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)
        recoveryPlanActivity.loadMetricWeekData(this, metricAdapter, currentWeek)

        // Setup the progress bar and text
        weeklyProgressBar = findViewById(R.id.weeklyGoalProgress)
        weeklyProgressText = findViewById(R.id.weeklyPercentageText)
        calculateWeeklyProgress(currentWeek)

        // Initialize edit goals button
        editGoalsBtn = findViewById(R.id.editGoalsBtn)
        editGoalsBtn.setOnClickListener { startActivity(Intent(this, RecoveryPlanActivity::class.java)) }

        // Initialize buttons & text related to goal completion
        goalsCompletedText = findViewById(R.id.goalsCompletedText)
        noGoalsSetText = findViewById(R.id.noGoalsSetText)
        setGoalsBtn = findViewById(R.id.setGoalsBtn)
        setGoalsBtn.setOnClickListener { startActivity(Intent(this, RecoveryPlanActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())

        // Load weekly exercise and metric data
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)
        recoveryPlanActivity.loadMetricWeekData(this, metricAdapter, currentWeek)

        // Ensure progress is updated when the activity comes to the foreground
        calculateWeeklyProgress(currentWeek)

        // Update visibility of recycler view table after the adapter processes the data
        recyclerViewUpdatedCounter = AtomicInteger(2) // Reset the counter
        exerciseRecyclerView.post {
            ExerciseUtil.updateRecyclerViewOverviewVisibility(exerciseAdapter, exerciseTableLayout)
            checkIfBothRecyclerViewsCompleted()
        }
        metricRecyclerView.post {
            ExerciseUtil.updateRecyclerViewOverviewVisibility(metricAdapter, metricTableLayout)
            checkIfBothRecyclerViewsCompleted()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Toggle sidebar state
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START) // Close if it's open
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)  // Open if it's closed
                }
                true
            }
            R.id.action_settings -> {
                // Open settings activity
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_toolbar_menu, menu)
        return true
    }

    override fun onDestroy() {
        if (isBluetoothServiceBound) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    override fun onClickStartExerciseWithoutWarning(exercise: Exercise) {
        recoveryPlanActivity.onStartExerciseActivity(this, exercise)
    }

    override fun onClickStartMetricWithoutWarning(metric: Metric) {
        recoveryPlanActivity.onStartMetricActivity(this, metric)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothService.initBluetooth()
            } else {
                GeneralUtil.showToast(this, layoutInflater, "Bluetooth permissions are required")
            }
        }
    }

    /**
     * Calculates the progress of exercise completion for the given week.
     *
     * @param currentWeek chosen week to calculate progress
     */
    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun calculateWeeklyProgress(currentWeek: String) {
        val exerciseGoalsForCurrentWeek = exerciseAdapter.getExercises()
        val metricGoalsForCurrentWeek = metricAdapter.getMetrics()

        // Calculate weekly progress of goals
        val weeklyProgress = recoveryPlanActivity.calculateWeeklyProgress(this, exerciseAdapter, metricAdapter, exerciseGoalsForCurrentWeek, metricGoalsForCurrentWeek, currentWeek)
        val truncatedWeeklyProgress = String.format("%.2f", weeklyProgress)

        // Update progress bar and text
        weeklyProgressBar.progress = weeklyProgress.toInt()
        weeklyProgressText.text = "$truncatedWeeklyProgress%"
    }

    /**
     * Sets the visibility of various text views and buttons related to
     * when no goals are set or all goals are completed.
     *
     * @param goalsCompleted boolean if all goals are completed
     * @param noGoals boolean if no goals are set
     */
    private fun setGoalsTextVisibility(goalsCompleted: Boolean = false, noGoals: Boolean = false) {
        goalsCompletedText.visibility = if (goalsCompleted) View.VISIBLE else View.GONE

        if (noGoals) {
            noGoalsSetText.visibility = View.VISIBLE
            setGoalsBtn.visibility = View.VISIBLE
        } else {
            noGoalsSetText.visibility = View.GONE
            setGoalsBtn.visibility = View.GONE
        }
    }

    /**
     * Sets the visibility of various text views and buttons after both recycler views have completed updating.
     */
    private fun checkIfBothRecyclerViewsCompleted() {
        if (recyclerViewUpdatedCounter.decrementAndGet() == 0) {
            // Both recycler views have finished their post actions
            if (exerciseTableLayout.visibility == View.GONE && metricTableLayout.visibility == View.GONE) {
                editGoalsBtn.visibility = View.GONE

                // Check goals completed percentage
                val currentProgress = weeklyProgressBar.progress
                if (currentProgress >= 100) {
                    // All goals completed
                    setGoalsTextVisibility(goalsCompleted = true)
                } else {
                    // No goals set
                    setGoalsTextVisibility(noGoals = true)
                }
            } else {
                editGoalsBtn.visibility = View.VISIBLE
                setGoalsTextVisibility()
            }

            // Reset the counter
            recyclerViewUpdatedCounter = AtomicInteger(2)
        }
    }
}
