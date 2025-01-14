package com.example.adaptanklebrace

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.RecoveryPlanActivity.Companion.RECOVERY_PLAN_PREFERENCE
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.services.BluetoothService
import com.google.android.material.navigation.NavigationView
import java.util.Calendar

class MainActivity : AppCompatActivity(), RecoveryPlanOverviewTableRowAdapter.MainActivityCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var weeklyProgressBar: ProgressBar
    private lateinit var weeklyProgressText: TextView
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var editGoalsBtn: Button

    private lateinit var exerciseAdapter: RecoveryPlanOverviewTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    private var settingsActivity = SettingsActivity()
    @RequiresApi(Build.VERSION_CODES.Q)
    private var recoveryPlanActivity = RecoveryPlanActivity()
    private lateinit var bluetoothService: BluetoothService
    private var isBluetoothServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothService.LocalBinder).getService()
            isBluetoothServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBluetoothServiceBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the app theme
        // Check saved preference for night mode
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isNightMode = settingsActivity.getPreference(sharedPreferences, "nightMode", false)
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

        // todo: if there are no goals set for that week, show button to set goals which shows the add goals fragment
        // todo: fix bug with row percentage completed not updating after recovery data is added

        // Setup exercise recovery plan table overview
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup the adapter
        exerciseAdapter = RecoveryPlanOverviewTableRowAdapter(exercises, this)
        exerciseRecyclerView.adapter = exerciseAdapter

        // Load data for current week on activity start
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)

        // Setup the progress bar and text
        weeklyProgressBar = findViewById(R.id.weeklyGoalProgress)
        weeklyProgressText = findViewById(R.id.weeklyPercentageText)
        calculateWeeklyProgress(currentWeek)

        // Initialize edit goals button
        editGoalsBtn = findViewById(R.id.editGoalsBtn)
        editGoalsBtn.setOnClickListener { startActivity(Intent(this, RecoveryPlanActivity::class.java)) }
    }

    // Call calculateWeeklyProgress() whenever the activity is resumed
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())

        // Load weekly exercise data
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)

        // Ensure progress is updated when the activity comes to the foreground
        calculateWeeklyProgress(currentWeek)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClickStartExerciseWithoutWarning(exercise: Exercise) {
        recoveryPlanActivity.onStartExerciseActivity(this, exercise)
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun calculateWeeklyProgress(currentWeek: String) {
        val exerciseGoalsForCurrentWeek = ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).getExercisesForDate(currentWeek)

        // Get reference to RecoveryPlanActivity
        val weeklyProgress = recoveryPlanActivity.calculateWeeklyProgress(this, exerciseAdapter, exerciseGoalsForCurrentWeek, currentWeek)
        val truncatedWeeklyProgress = String.format("%.2f", weeklyProgress)

        // Update progress bar and text
        weeklyProgressBar.progress = weeklyProgress.toInt()
        weeklyProgressText.text = "$truncatedWeeklyProgress%"
    }

    /*** BLUETOOTH INITIALIZATION  ***/
    // todo: add this initialization to other activities that can start exercises
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 2

    @RequiresApi(Build.VERSION_CODES.S)
    fun checkAndRequestBluetoothPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                BLUETOOTH_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothService.initBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
