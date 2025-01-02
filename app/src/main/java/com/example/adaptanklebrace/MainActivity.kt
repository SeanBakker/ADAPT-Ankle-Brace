package com.example.adaptanklebrace

import android.Manifest
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.adaptanklebrace.fragments.ConnectDeviceFragment
import com.example.adaptanklebrace.services.BluetoothService
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences

    private var settingsActivity = SettingsActivity()
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
                R.id.nav_recovery_progress -> startActivity(Intent(this, RecoveryProgressActivity::class.java))
                R.id.nav_notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
            }
            drawerLayout.closeDrawers() // Close the sidebar
            true
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Start exercise from button (triggers connection to device)
        val startExerciseButton: Button = findViewById(R.id.startExerciseBtn)
        startExerciseButton.setOnClickListener {
            val connectDeviceFragment = ConnectDeviceFragment()
            connectDeviceFragment.show(supportFragmentManager, "connect_device")
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




    /*** BLUETOOTH INITIALIZATION  ***/
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
