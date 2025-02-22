package com.example.adaptanklebrace

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.adaptanklebrace.utils.GeneralUtil
import com.example.adaptanklebrace.utils.SharedPreferencesUtil

class SettingsActivity : BaseActivity() {

    // Checkboxes in settings page
    private lateinit var bluetoothPermissionCheckbox: CheckBox
    private lateinit var notificationsCheckbox: CheckBox
    private lateinit var nightModeCheckbox: CheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1002

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
            if (isChecked) {
                // Check if the app already has bluetooth permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted
                    SharedPreferencesUtil.savePreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, true)
                    bluetoothEnabled = true
                } else {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                        BLUETOOTH_PERMISSION_REQUEST_CODE
                    )
                }
            } else {
                // Handle the case when bluetooth is disabled
                SharedPreferencesUtil.savePreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, false)
                bluetoothEnabled = false
            }
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
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Notification permission granted
                    SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, true)
                    notificationsEnabled = true
                    GeneralUtil.showToast(this, layoutInflater, "Notification permission enabled")
                } else {
                    // Notification permission denied
                    SharedPreferencesUtil.savePreference(sharedPreferences, NOTIFICATIONS_PERMISSION_KEY, false)
                    notificationsCheckbox.isChecked = false
                    notificationsEnabled = false
                    GeneralUtil.showToast(this, layoutInflater, "Notification permission denied")
                }
            }

            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Bluetooth permission granted
                    SharedPreferencesUtil.savePreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, true)
                    bluetoothEnabled = true
                    GeneralUtil.showToast(this, layoutInflater, "Bluetooth permission granted")
                } else {
                    // Bluetooth permission denied
                    SharedPreferencesUtil.savePreference(sharedPreferences, BLUETOOTH_PERMISSION_KEY, false)
                    bluetoothPermissionCheckbox.isChecked = false
                    bluetoothEnabled = false
                    GeneralUtil.showToast(this, layoutInflater, "Bluetooth permission denied")
                }
            }
        }
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
