package com.example.adaptanklebrace

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var bluetoothPermissionCheckbox: CheckBox
    private lateinit var notificationsCheckbox: CheckBox
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

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
            onBackPressed() // Go back to the previous activity
        }

        // Find views by ID
        bluetoothPermissionCheckbox = findViewById(R.id.bluetoothPermissionCheckbox)
        notificationsCheckbox = findViewById(R.id.notificationsCheckbox)

        // Load and set saved preferences
        bluetoothPermissionCheckbox.isChecked = sharedPreferences.getBoolean("bluetoothPermission", false)
        notificationsCheckbox.isChecked = sharedPreferences.getBoolean("notifications", false)

        // Set listeners for checkboxes to save changes
        bluetoothPermissionCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("bluetoothPermission", isChecked)
        }

        notificationsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            savePreference("notifications", isChecked)
        }
    }

    private fun savePreference(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}
