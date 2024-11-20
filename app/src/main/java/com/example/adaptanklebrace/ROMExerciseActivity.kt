package com.example.adaptanklebrace

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ROMExerciseActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var percentageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rom_exercise)

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

        // Simulated foot angle update
        updateProgress(100)
    }

    private fun updateProgress(anglePercentage: Int) {
        progressBar.progress = anglePercentage
        percentageText.text = "$anglePercentage%"
    }
}
