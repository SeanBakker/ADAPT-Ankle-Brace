package com.example.adaptanklebrace.fragments

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.adaptanklebrace.utils.GeneralUtil

class ConnectDeviceFragment(
    private val context: Context,
    private val targetActivity: Class<*>,
    private val exercise: Exercise
) : DialogFragment() {

    private var isManuallyDismissed: Boolean = false
    private var isTensionCorrect: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connecting_device, container, false)

        val tensionText: TextView = view.findViewById(R.id.tension)
        tensionText.text = exercise.tension.toString()

        // Start the Bluetooth service
        val serviceIntent = Intent(context, BluetoothService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        val bluetoothService = BluetoothService.instance
        bluetoothService?.resetLiveData()

        val connectButton: Button = view.findViewById(R.id.connectDeviceBtn)
        connectButton.setOnClickListener {
            connectToDevice(activity, bluetoothService)
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

    private fun connectToDevice(currentActivity: Activity?, bluetoothService: BluetoothService?) {
        currentActivity?.let {
            if (bluetoothService == null) {
                GeneralUtil.showToast(context, layoutInflater, "Bluetooth service not available")
                return
            }

            // Check and request bluetooth permissions to ADAPT device
            if (bluetoothService.checkAndRequestBluetoothPermissions(requireActivity())) {
                if (bluetoothService.connectToBluetoothDevice(it)) {
                    Thread.sleep(1000) // Wait for device to load
                    bluetoothService.writeDeviceData("ready")

                    // Check the device for the configured tension level
                    Handler(Looper.getMainLooper()).postDelayed({
                        bluetoothService.readDeviceData()
                    }, 50)
                    Thread.sleep(100) // Wait for device to load

                    bluetoothService.readDeviceData()
                    bluetoothService.deviceLiveData.observe(this) { pair ->
                        pair?.let { value ->
                            val deviceTension = value.first
                            if (deviceTension != exercise.tension && deviceTension in 1..10) {
                                val tensionLevelWarningFragment = TensionLevelWarningFragment(
                                    it,
                                    targetActivity,
                                    bluetoothService,
                                    exercise,
                                    deviceTension
                                )
                                tensionLevelWarningFragment.show(
                                    parentFragmentManager,
                                    "tension_level_warning"
                                )
                                isTensionCorrect = false
                            } else {
                                isTensionCorrect = true
                            }
                        }
                    }

                    // Only show other dialogs if tension is correct
                    if (isTensionCorrect) {
                        // Send test rep flag to device to prepare exercise data collection (of test rep)
                        if (targetActivity == StartSetActivity::class.java) {
                            val testRepFragment = TestRepFragment(it, bluetoothService, exercise)
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
                            val startSetIntent = Intent(it, targetActivity)
                            val parcelableExercise = exercise as Parcelable
                            startSetIntent.putExtra(ExerciseInfo.EXERCISE_KEY, parcelableExercise)
                            ContextCompat.startActivity(it, startSetIntent, null)
                        }
                    }

                    // Manually close fragment
                    isManuallyDismissed = true
                    dismiss() // Close the dialog
                }
            } else {
                GeneralUtil.showToast(context, layoutInflater, "Bluetooth permissions disabled")
            }
        }
    }
}
