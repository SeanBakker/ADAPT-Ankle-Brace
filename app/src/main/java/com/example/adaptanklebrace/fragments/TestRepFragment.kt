package com.example.adaptanklebrace.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.StartSetActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.services.BluetoothService

class TestRepFragment(
    private val context: Context,
    private val bluetoothService: BluetoothService,
    private val exercise: Exercise,
) : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_test_rep, container, false)

        // Update the exercise name
        val exerciseNameText: TextView = view.findViewById(R.id.exerciseName)
        exerciseNameText.text = exercise.name

        // Initialize the progress
        val progressLiveDataText: TextView = view.findViewById(R.id.testRepProgressLiveDataText)
        val progressBar: ProgressBar = view.findViewById(R.id.testRepProgressBar)
        updateProgress(progressBar, progressLiveDataText, 0.0)
        handler.removeCallbacks(updateAngleTask)
        bluetoothService.deviceLiveData.removeObservers(viewLifecycleOwner)
        bluetoothService.resetLiveData()
        Thread.sleep(1000) // Wait for device to load

        // Configure start button
        val startTestRepButton: Button = view.findViewById(R.id.startTestRepBtn)
        startTestRepButton.setOnClickListener {
            startTestRep(view)
        }

        // Configure end button
        val endTestRepButton: Button = view.findViewById(R.id.endTestRepBtn)
        endTestRepButton.setOnClickListener {
            endTestRep()
        }

        return view
    }

    // Collect live data of test rep
    private fun startTestRep(view: View) {
        val progressLiveDataText: TextView = view.findViewById(R.id.testRepProgressLiveDataText)
        val progressBar: ProgressBar = view.findViewById(R.id.testRepProgressBar)

        // Select flag to send to the device
        val testRepFlag = when (exercise.name) {
            ExerciseType.PLANTAR_FLEXION.exerciseName -> "test_plantar"
            ExerciseType.DORSIFLEXION.exerciseName -> "test_dorsiflexion"
            ExerciseType.INVERSION.exerciseName -> "test_inversion"
            ExerciseType.EVERSION.exerciseName -> "test_eversion"
            else -> "no_test_rep"
        }
        bluetoothService.writeDeviceData(testRepFlag)

        // Periodically read angle from ADAPT device
        handler.post(updateAngleTask)
        bluetoothService.deviceLiveData.observe(viewLifecycleOwner) { pair ->
            pair?.let {
                updateProgress(progressBar, progressLiveDataText, it.first.toDouble())
                Log.i("TEST_REP", "Angle data received: ${it.first}")
            }
        }
    }

    // Complete test rep and trigger sets to begin
    private fun endTestRep() {
        Thread.sleep(1000) // Wait for device to load

        // Cleanup
        bluetoothService.deviceLiveData.removeObservers(viewLifecycleOwner)
        handler.removeCallbacks(updateAngleTask)

        // Send finish flag to device to end test rep
        bluetoothService.writeDeviceData("test_complete")
        Thread.sleep(2000) // Wait for device to load

        // Start target activity to perform sets
        val startSetIntent = Intent(context, StartSetActivity::class.java)
        val parcelableExercise = exercise as Parcelable
        startSetIntent.putExtra(ExerciseInfo.EXERCISE_KEY, parcelableExercise)

        // Send the min/max range values found from test rep
        bluetoothService.readDeviceData()

        // Start activity
        ContextCompat.startActivity(context, startSetIntent, null)
        dismiss() // Close the dialog
    }

    @SuppressLint("DefaultLocale")
    private fun updateProgress(progressBar: ProgressBar, progressLiveDataText: TextView, anglesDegrees: Double) {
        progressBar.progress = anglesDegrees.toInt()
        progressLiveDataText.text = String.format("%.1f°", anglesDegrees)
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
