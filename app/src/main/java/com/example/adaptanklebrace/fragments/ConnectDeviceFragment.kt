package com.example.adaptanklebrace.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.services.BluetoothService
import com.example.adaptanklebrace.utils.ExerciseUtil

class ConnectDeviceFragment(
    private val context: Context,
    private val targetActivity: Class<out Activity>,
    private val exercise: Exercise
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connecting_device, container, false)

        val tensionText: TextView = view.findViewById(R.id.tension)
        tensionText.text = exercise.tension.toString()

        val connectButton: Button = view.findViewById(R.id.connectDeviceBtn)
        connectButton.setOnClickListener {
            connectToDevice()
        }

        return view
    }

    private fun connectToDevice() {
        // Get reference to the current activity
        val currentActivity = activity

        currentActivity?.let {
            // Start the Bluetooth service
            val serviceIntent = Intent(it, BluetoothService::class.java)
            ContextCompat.startForegroundService(it, serviceIntent)
            val bluetoothService = BluetoothService.instance
            if (bluetoothService == null) {
                ExerciseUtil.showToast(context, layoutInflater, "Bluetooth service not available")
                return
            }

            // Check and request bluetooth permissions to ADAPT device
            if (true) { // it.checkAndRequestBluetoothPermissions()
                if (true) { // bluetoothService.connectToBluetoothDevice(it)
                    bluetoothService.writeDeviceData("ready")
                    dismiss() // Close the dialog

                    // Check the device for the configured tension level
                    // todo: test functionality with the device itself
                    bluetoothService.readDeviceData()
                    bluetoothService.deviceLiveData.value?.toInt()?.let { deviceTension ->
                        Log.i("TENSION", "Tension received from device: $deviceTension")

                        // Trigger tension level warning fragment if tension is configured incorrectly
                        if (deviceTension != exercise.tension) {
                            val tensionLevelWarningFragment = TensionLevelWarningFragment(exercise, deviceTension)
                            tensionLevelWarningFragment.show(childFragmentManager, "tension_level_warning")
                        }
                    } ?: run {
                        Log.e("Bluetooth", "Failed to read tension level from device")
                    }

                    // Trigger fragment for performing test rep
                    // todo: implement test rep fragment
                    // todo: not all exercises will have the test rep

                    // After test rep is complete, start target activity to perform sets
                    val startSetIntent = Intent(it, targetActivity)
                    val parcelableExercise = exercise as Parcelable
                    startSetIntent.putExtra(ExerciseInfo.EXERCISE_KEY, parcelableExercise)
                    ContextCompat.startActivity(it, startSetIntent, null)
                }
            } else {
                ExerciseUtil.showToast(context, layoutInflater, "Bluetooth permissions disabled")
            }
        }
    }
}
