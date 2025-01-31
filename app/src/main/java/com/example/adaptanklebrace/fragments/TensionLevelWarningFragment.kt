package com.example.adaptanklebrace.fragments

import android.content.Context
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
import com.example.adaptanklebrace.GaitTestExerciseActivity
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.ROMExerciseActivity
import com.example.adaptanklebrace.StartSetActivity
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.services.BluetoothService

class TensionLevelWarningFragment(
    private val context: Context,
    private val targetActivity: Class<*>,
    private val bluetoothService: BluetoothService,
    private val exercise: Exercise,
    private val actualTensionLevel: Int
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tension_level_warning, container, false)

        val expectedTensionText: TextView = view.findViewById(R.id.expectedTension)
        expectedTensionText.text = exercise.tension.toString()

        val actualTensionText: TextView = view.findViewById(R.id.actualTension)
        actualTensionText.text = actualTensionLevel.toString()

        val startExerciseButton: Button = view.findViewById(R.id.startExerciseBtn)
        startExerciseButton.setOnClickListener {
            startExercise()
        }

        return view
    }

    private fun startExercise() {
        // Send test rep flag to device to prepare exercise data collection (of test rep)
        if (targetActivity == StartSetActivity::class.java) {
            val testRepFragment = TestRepFragment(context, bluetoothService, exercise)
            testRepFragment.show(parentFragmentManager, "test_rep")
        } else {
            Thread.sleep(2000) // Wait for device to load

            // Send start flag to device to prepare exercise data collection (of sets)
            when (targetActivity) {
                ROMExerciseActivity::class.java -> bluetoothService.writeDeviceData("start_ROM")
                GaitTestExerciseActivity::class.java -> bluetoothService.writeDeviceData("start_Gait")
                else -> bluetoothService.writeDeviceData(getString(R.string.error))
            }

            // Start target activity to perform sets
            val startSetIntent = Intent(context, targetActivity)
            val parcelableExercise = exercise as Parcelable
            startSetIntent.putExtra(ExerciseInfo.EXERCISE_KEY, parcelableExercise)
            ContextCompat.startActivity(context, startSetIntent, null)
        }

        dismiss() // Close the dialog
    }
}
