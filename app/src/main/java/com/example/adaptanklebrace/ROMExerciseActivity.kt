package com.example.adaptanklebrace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.adaptanklebrace.services.BluetoothService

class ROMExerciseActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var percentageText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rom_exercise)

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        if (BluetoothService.instance == null) {
            Toast.makeText(this, "Bluetooth service not available", Toast.LENGTH_SHORT).show()
            finish() // Exit activity
            return
        }
        bluetoothService = BluetoothService.instance!!

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.romExerciseToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.rom_exercise)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed() // Go back to the previous activity
        }

        // Setup the progress bar and text
        progressBar = findViewById(R.id.circularProgress)
        percentageText = findViewById(R.id.percentageText)

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
    }

    private fun updateProgress(angleDegrees: Int) {
        progressBar.progress = angleDegrees
        percentageText.text = "$angleDegrees°"
    }

    private val updateAngleTask = object : Runnable {
        override fun run() {
            // Read characteristic (device) data from MainActivity
            bluetoothService.readDeviceData()
            bluetoothService.deviceLiveData.value?.let { updateProgress(it) }
            handler.postDelayed(this, 50)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateAngleTask)
        super.onDestroy()
    }
}
