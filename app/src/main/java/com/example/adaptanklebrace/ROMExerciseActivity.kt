package com.example.adaptanklebrace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.adaptanklebrace.services.BluetoothService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ROMExerciseActivity : AppCompatActivity() {

    private lateinit var flexionProgressBar: ProgressBar
    private lateinit var inversionProgressBar: ProgressBar
    private lateinit var flexionPercentageText: TextView
    private lateinit var inversionPercentageText: TextView
    private lateinit var flexionTotalROM: TextView
    private lateinit var inversionTotalROM: TextView
    private lateinit var bluetoothService: BluetoothService
    private val handler = Handler(Looper.getMainLooper())

    private val angleDeviceData: List<Pair<Double, Double>> = listOf(
        Pair(125.45, 25.11),Pair(133.91, 22.06),Pair(138.14, 20.44),Pair(140.37, 19.37),Pair(141.35, 19.11),Pair(141.8, 19.27),Pair(141.97, 19.96),
        Pair(142.08, 20.33),Pair(142.24, 20.17),Pair(141.93, 19.62),Pair(141.88, 19.7),Pair(142.11, 18.93),Pair(142.38, 18.83),Pair(143.19, 18.64),Pair(142.65, 18.9),
        Pair(141.53, 22.3),Pair(139.18, 21.73),Pair(134.95, 21.78),Pair(132.02, 23.05),Pair(129.87, 23.11),Pair(128.64, 21.78),Pair(124.8, 23.67),Pair(123.55, 22.3),
        Pair(119.52, 21.89),Pair(119.51, 23.37),Pair(117.79, 21.81),Pair(115.72, 21.48),Pair(112.74, 19.64),Pair(109.34, 20.83),Pair(106.41, 23.23),
        Pair(107.87, 22.47),Pair(107.12, 25.64),Pair(106.41, 21.94),Pair(109.62, 22.44),Pair(111.78, 19.03),Pair(114.78, 14.55),Pair(119.66, 9.53),Pair(121.82, 8.23),
        Pair(124.92, 6.96),Pair(126.68, 7.31),Pair(128.89, 7.4),Pair(128.34, 3.35),Pair(134.29, 7.11),Pair(143.34, 7.59),Pair(146.09, 9.7),Pair(147.03, 11.7),
        Pair(150.88, 11.36),Pair(154.12, 12.11),Pair(154.7, 16.74),Pair(156.31, 20.64),Pair(156.1, 23.91),Pair(149.71, 18.78),Pair(148.78, 24.15),Pair(138.63, 23.46),
        Pair(134.09, 22.55),Pair(129.73, 22.99),Pair(125.38, 20.85),Pair(121.54, 22.67),Pair(117.0, 21.51),Pair(115.77, 22.51), Pair(113.75, 21.79),Pair(112.62, 21.66),
        Pair(112.57, 20.47),Pair(112.98, 18.36),Pair(112.62, 18.3),Pair(110.72, 13.99),Pair(108.85, 20.86),Pair(106.78, 23.39),Pair(107.19, 24.03),Pair(109.8, 20.93),
        Pair(100.48, 30.64),Pair(111.13, 22.04),Pair(113.63, 11.53),Pair(118.27, 7.76),Pair(120.34, 8.76),Pair(125.73, 8.09),Pair(130.96, 7.99),Pair(128.2, 10.21),
        Pair(138.09, 12.45),Pair(139.16, 10.22),Pair(143.82, 10.14),Pair(147.18, 8.53),Pair(149.8, 11.59),Pair(154.53, 14.32),Pair(155.66, 17.99),Pair(155.79, 21.66),
        Pair(153.47, 19.73),Pair(154.12, 22.2),Pair(148.57, 22.86),Pair(146.26, 19.44),Pair(147.71, 23.88),Pair(139.9, 26.21),Pair(136.05, 25.18),Pair(128.85, 24.9),
        Pair(125.63, 24.36),Pair(118.82, 25.8),Pair(116.31, 24.37),Pair(114.41, 24.23),Pair(113.42, 23.04),Pair(115.18, 18.86),
    )
    private val angleROMData: Pair<Double, Double> = Pair(55.83, 27.29)

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

        // Setup the progress bars and text
        flexionProgressBar = findViewById(R.id.flexionProgress)
        inversionProgressBar = findViewById(R.id.inversionProgress)
        flexionPercentageText = findViewById(R.id.flexionPercentageText)
        inversionPercentageText = findViewById(R.id.inversionPercentageText)
        flexionTotalROM = findViewById(R.id.flexionROMTotal)
        inversionTotalROM = findViewById(R.id.inversionROMTotal)

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
    }

    private fun updateProgress(anglesDegrees: Pair<Double, Double>) {
        val (flexionAngle, inversionAngle) = anglesDegrees
        flexionProgressBar.progress = flexionAngle.toInt()
        flexionPercentageText.text = "$flexionAngle°"

        inversionProgressBar.progress = inversionAngle.toInt()
        inversionPercentageText.text = "$inversionAngle°"
    }

    private fun updateTotalROM(anglesDegrees: Pair<Double, Double>) {
        val (flexionTotal, inversionTotal) = anglesDegrees
        flexionTotalROM.text = "$flexionTotal°"
        inversionTotalROM.text = "$inversionTotal°"
    }

    private val updateAngleTask = object : Runnable {
        override fun run() {
            // Read characteristic (device) data from MainActivity
            bluetoothService.readDeviceData()
            bluetoothService.deviceLiveData.value?.toInt().let {
                if (it != null) {
                    updateProgress(Pair(it.toDouble(), it.toDouble()))
                    Log.i("ANGLE", "Angle data received: $it")
                }
            }
            handler.postDelayed(this, 50)

            // Send demo data
            /*
            GlobalScope.launch(Dispatchers.Main) {
                delay(2000)
                for (anglePair in angleDeviceData) {
                    updateProgress(anglePair)
                    delay(50)
                }
                updateTotalROM(angleROMData)
            }
            */
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateAngleTask)
        super.onDestroy()
    }
}
