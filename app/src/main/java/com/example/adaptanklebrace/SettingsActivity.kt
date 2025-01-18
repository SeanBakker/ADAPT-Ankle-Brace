package com.example.adaptanklebrace

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.SharedPreferencesUtil

class SettingsActivity : AppCompatActivity() {

    // Checkboxes in settings page
    private lateinit var bluetoothPermissionCheckbox: CheckBox
    private lateinit var notificationsCheckbox: CheckBox
    private lateinit var nightModeCheckbox: CheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    companion object {
        const val SETTINGS_PREFERENCE = "AppSettings"
        const val BLUETOOTH_PERMISSION_KEY = "bluetoothPermission"
        const val NOTIFICATIONS_PERMISSION_KEY = "NotificationsPermission"
        const val NIGHT_MODE_KEY = "nightMode"

        var bluetoothEnabled: Boolean = false
        var notificationsEnabled: Boolean = false
        var nightMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferencesUtil
        sharedPreferences = getSharedPreferences(SETTINGS_PREFERENCE, MODE_PRIVATE)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed() // Go back to the previous activity
        }

        // Find views by ID
        bluetoothPermissionCheckbox = findViewById(R.id.bluetoothPermissionCheckbox)
        notificationsCheckbox = findViewById(R.id.notificationsCheckbox)
        nightModeCheckbox = findViewById(R.id.nightModeCheckbox)

        // Load and set saved preferences
        bluetoothPermissionCheckbox.isChecked = SharedPreferencesUtil.getPreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, false)
        notificationsCheckbox.isChecked = SharedPreferencesUtil.getPreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, false)
        nightModeCheckbox.isChecked = SharedPreferencesUtil.getPreference(sharedPreferences, NIGHT_MODE_KEY, false)

        bluetoothEnabled = bluetoothPermissionCheckbox.isChecked
        notificationsEnabled = notificationsCheckbox.isChecked
        nightMode = nightModeCheckbox.isChecked

        // Set listeners for checkboxes to save changes
        bluetoothPermissionCheckbox.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesUtil.savePreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, isChecked)
            bluetoothEnabled = isChecked
        }

        notificationsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if the app already has notification permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted
                    SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, true)
                    notificationsEnabled = true
                } else {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            } else {
                // Handle the case when notifications are disabled
                SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, false)
                notificationsEnabled = false
            }
        }

        nightModeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesUtil.savePreference(sharedPreferences, NIGHT_MODE_KEY, isChecked)
            changeAppTheme(isChecked)
            nightMode = isChecked
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, true)
                notificationsEnabled = true
                ExerciseUtil.showToast(this, layoutInflater, "Notifications enabled")
            } else {
                // Permission denied
                SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, false)
                notificationsEnabled = false
                ExerciseUtil.showToast(this, layoutInflater, "Notification permission denied")
            }
        }

        // todo: add request for bluetooth permissions
    }

    // Change the app theme between Light/Night modes
    fun changeAppTheme(isNightMode: Boolean) {
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
