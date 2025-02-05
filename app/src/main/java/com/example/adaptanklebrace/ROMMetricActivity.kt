package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.GeneralUtil

class ROMMetricActivity : AppCompatActivity() {

    private lateinit var flexionProgressBar: ProgressBar
    private lateinit var inversionProgressBar: ProgressBar
    private lateinit var flexionPercentageText: TextView
    private lateinit var inversionPercentageText: TextView
    private lateinit var flexionTotalROM: TextView
    private lateinit var inversionTotalROM: TextView
    private lateinit var bluetoothService: BluetoothService
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rom_metric)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.romExerciseToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.rom_metric)

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

        // Setup the progress bars and text
        flexionProgressBar = findViewById(R.id.flexionProgress)
        inversionProgressBar = findViewById(R.id.inversionProgress)
        flexionPercentageText = findViewById(R.id.flexionPercentageText)
        inversionPercentageText = findViewById(R.id.inversionPercentageText)
        flexionTotalROM = findViewById(R.id.flexionROMTotal)
        inversionTotalROM = findViewById(R.id.inversionROMTotal)

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
        bluetoothService.deviceLiveData.observe(this) { pair ->
            pair?.let {
                updateProgress(Pair(it.first.toDouble(), it.second.toDouble()))
                Log.i("ANGLE", "Angle data received: (${it.first}, ${it.second})")
            }
        }

        // todo: add sets and start/end buttons to be able to calculate the total ROM at the end after each set
        //updateTotalROM()
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(anglesDegrees: Pair<Double, Double>) {
        val (flexionAngle, inversionAngle) = anglesDegrees
        flexionProgressBar.progress = flexionAngle.toInt()
        flexionPercentageText.text = "$flexionAngle°"

        inversionProgressBar.progress = inversionAngle.toInt()
        inversionPercentageText.text = "$inversionAngle°"
    }

    @SuppressLint("SetTextI18n")
    private fun updateTotalROM(anglesDegrees: Pair<Double, Double>) {
        val (flexionTotal, inversionTotal) = anglesDegrees
        flexionTotalROM.text = "$flexionTotal°"
        inversionTotalROM.text = "$inversionTotal°"
    }

    private val updateAngleTask = object : Runnable {
        override fun run() {
            // Read characteristic (device) data
            bluetoothService.readDeviceData()
            handler.postDelayed(this, 50)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateAngleTask)
        super.onDestroy()
    }
}
