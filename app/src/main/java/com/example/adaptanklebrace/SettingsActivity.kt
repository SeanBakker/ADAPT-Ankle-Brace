package com.example.adaptanklebrace

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    // Checkboxes in settings page
    private lateinit var bluetoothPermissionCheckbox: CheckBox
    private lateinit var notificationsCheckbox: CheckBox
    private lateinit var weeklyGoalReminderCheckbox: CheckBox
    private lateinit var dailyExerciseReminderCheckbox: CheckBox
    private lateinit var nightModeCheckbox: CheckBox

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        var nightMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

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
        weeklyGoalReminderCheckbox = findViewById(R.id.weeklyGoalReminderCheckbox)
        dailyExerciseReminderCheckbox = findViewById(R.id.dailyExerciseReminderCheckbox)
        nightModeCheckbox = findViewById(R.id.nightModeCheckbox)

        // Load and set saved preferences
        bluetoothPermissionCheckbox.isChecked = getPreference(sharedPreferences, "bluetoothPermission", false)
        notificationsCheckbox.isChecked = getPreference(sharedPreferences, "notifications", false)
        weeklyGoalReminderCheckbox.isChecked = getPreference(sharedPreferences, "weeklyGoalReminder", false)
        dailyExerciseReminderCheckbox.isChecked = getPreference(sharedPreferences, "dailyExerciseReminder", false)
        nightModeCheckbox.isChecked = getPreference(sharedPreferences, "nightMode", false)

        // Set listeners for checkboxes to save changes
        bluetoothPermissionCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("bluetoothPermission", isChecked)
        }
        notificationsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("notifications", isChecked)
        }
        weeklyGoalReminderCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("weeklyGoalReminder", isChecked)
        }
        dailyExerciseReminderCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("dailyExerciseReminder", isChecked)
        }
        nightModeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("nightMode", isChecked)
            changeAppTheme(isChecked)
            nightMode = isChecked
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

    // Get settings preference from app storage
    fun getPreference(sharedPreferences: SharedPreferences, key: String, value: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, value)
    }

    // Save settings preference to app storage
    private fun savePreference(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}
