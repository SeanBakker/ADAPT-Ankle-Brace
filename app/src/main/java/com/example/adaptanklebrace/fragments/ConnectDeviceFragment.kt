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
import com.example.adaptanklebrace.R
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
    private var isTensionCorrect: Boolean = false

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

        // todo: fix bug with staying connected to bluetooth service even when no other activities open

        if (!isManuallyDismissed) {
            val bluetoothService = BluetoothService.instance
            bluetoothService?.disconnect() // Ensure Bluetooth service disconnects
        }
    }

    private fun connectToDevice(currentActivity: Activity?, bluetoothService: BluetoothService?) {
        currentActivity?.let {
            if (bluetoothService == null) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.bluetoothServiceNotAvailableToast))
                return
            }

            // Check and request bluetooth permissions to ADAPT device
            if (/*true || */bluetoothService.checkAndRequestBluetoothPermissions(requireActivity())) {
                if (/*true || */bluetoothService.connectToBluetoothDevice(it)) {
                    bluetoothService.resetLiveData() // Reset live data before reading tension

                    Thread.sleep(2000) // Wait for device to load
                    bluetoothService.writeDeviceData("ready")
                    Thread.sleep(2000) // Wait for device to load

                    // Check the device for the configured tension level
                    Handler(Looper.getMainLooper()).postDelayed({
                        bluetoothService.readDeviceData()
                    }, 50)
                    Thread.sleep(500) // Wait for device to load

                    bluetoothService.readDeviceData()
                    bluetoothService.deviceLiveData.observe(this) { pair ->
                        pair?.let { value ->
                            val deviceTension1 = value.first.toInt()
                            val deviceTension2 = value.second.toInt()

                            /* Check if the tension level may be correct (330 degrees potentiometer)
                                - 1 and 5 will appear as the same
                                - 2 and 6 will appear as the same
                             */
                            val tensionLevel1Correct: Boolean = if (deviceTension1 != exercise.tension && deviceTension1 in 0..6) {
                                when (exercise.tension) {
                                    5 -> { deviceTension1 == 1 }
                                    6 -> { deviceTension1 == 2 }
                                    else -> false
                                }
                            } else {
                                true
                            }
                            val tensionLevel2Correct: Boolean = if (deviceTension2 != exercise.tension && deviceTension2 in 0..6) {
                                when (exercise.tension) {
                                    5 -> { deviceTension2 == 1 }
                                    6 -> { deviceTension2 == 2 }
                                    else -> false
                                }
                            } else {
                                true
                            }

                            if (!tensionLevel1Correct || !tensionLevel2Correct) {
                                val tensionLevelWarningFragment = TensionLevelWarningFragment(
                                    it,
                                    targetActivity,
                                    bluetoothService,
                                    exercise,
                                    deviceTension1,
                                    deviceTension2
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
                        bluetoothService.resetLiveData() // Reset live data after tension is read
                        proceedAfterTensionCheck(isTensionCorrect, it, targetActivity, bluetoothService, exercise)
                    }
                }
            } else {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.bluetoothPermissionsDisabledToast))
            }
        }
    }

    private fun proceedAfterTensionCheck(isTensionCorrect: Boolean, it: Context, targetActivity: Class<*>, bluetoothService: BluetoothService, exercise: Exercise) {
        // Only show other dialogs if tension is correct
        if (isTensionCorrect) {
            // Send test rep flag to device to prepare exercise data collection (of test rep)
            if (targetActivity == StartSetActivity::class.java) {
                val testRepFragment = TestRepFragment(it, bluetoothService, exercise)
                testRepFragment.show(parentFragmentManager, "test_rep")
            } else {
                Thread.sleep(1000) // Wait for device to load

                // Send no_test_rep flag to device to skip test rep
                bluetoothService.writeDeviceData("no_test_rep")

                Thread.sleep(1000) // Wait for device to load

                // Start target activity to perform sets
                // Use METRIC_KEY since only metrics will start the target activity through this execution path
                val startSetIntent = Intent(it, targetActivity)
                val parcelableExercise = exercise as Parcelable
                startSetIntent.putExtra(ExerciseInfo.METRIC_KEY, parcelableExercise)
                ContextCompat.startActivity(it, startSetIntent, null)
                activity?.finish()
            }
        }
        // Manually close fragment
        isManuallyDismissed = true
        dismiss() // Close the dialog
    }
}
