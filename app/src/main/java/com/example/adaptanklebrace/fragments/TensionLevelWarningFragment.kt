package com.example.adaptanklebrace.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.StartSetActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.services.BluetoothService

class TensionLevelWarningFragment(
    private val context: Context,
    private val targetActivity: Class<*>,
    private val bluetoothService: BluetoothService,
    private val exercise: Exercise,
    private val actualTensionLevel1: Int,
    private val actualTensionLevel2: Int
) : DialogFragment() {

    private var isManuallyDismissed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tension_level_warning, container, false)

        val expectedTensionText: TextView = view.findViewById(R.id.expectedTension)
        expectedTensionText.text = exercise.tension.toString()

        val actualTensionText1: TextView = view.findViewById(R.id.actualTension1)
        actualTensionText1.text = actualTensionLevel1.toString() // plantar/dorsiflexion tensioner

        val actualTensionText2: TextView = view.findViewById(R.id.actualTension2)
        actualTensionText2.text = actualTensionLevel2.toString() // inversion/eversion tensioner

        val startButton: Button = view.findViewById(R.id.startBtn)
        startButton.setOnClickListener {
            startExerciseOrMetric()
        }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if (!isManuallyDismissed) {
            val bluetoothService = BluetoothService.instance
            bluetoothService?.disconnect() // Ensure Bluetooth service disconnects
        }
    }

    private fun startExerciseOrMetric() {
        // Send test rep flag to device to prepare exercise data collection (of test rep)
        if (targetActivity == StartSetActivity::class.java) {
            val testRepFragment = TestRepFragment(context, bluetoothService, exercise)
            testRepFragment.show(parentFragmentManager, "test_rep")
        } else {
            Thread.sleep(2000) // Wait for device to load

            // Send no_test_rep flag to device to skip test rep
            bluetoothService.writeDeviceData("no_test_rep")

            // Start target activity to perform sets
            // Use METRIC_KEY since only metrics will start the target activity through this execution path
            val startSetIntent = Intent(context, targetActivity)
            val parcelableExercise = exercise as Parcelable
            startSetIntent.putExtra(ExerciseInfo.METRIC_KEY, parcelableExercise)
            ContextCompat.startActivity(context, startSetIntent, null)
            activity?.finish()
        }

        bluetoothService.resetLiveData() // Reset live data after tension is read
        // Manually close fragment
        isManuallyDismissed = true
        dismiss() // Close the dialog
    }
}
