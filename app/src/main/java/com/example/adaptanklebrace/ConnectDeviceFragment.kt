package com.example.adaptanklebrace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.services.BluetoothService

class ConnectDeviceFragment : DialogFragment() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.connecting_device, container, false)

        val connectButton: Button = view.findViewById(R.id.connectDeviceBtn)
        connectButton.setOnClickListener {
            connectToDevice()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToDevice() {
        // Get reference to MainActivity
        val mainActivity = activity as? MainActivity

        mainActivity?.let {
            // Start the Bluetooth service
            val serviceIntent = Intent(it, BluetoothService::class.java)
            ContextCompat.startForegroundService(it, serviceIntent)
            val bluetoothService = BluetoothService.instance
            if (bluetoothService == null) {
                Toast.makeText(it, "Bluetooth service not available", Toast.LENGTH_SHORT).show()
                return
            }

            // Check and request bluetooth permissions to ADAPT device
            if (it.checkAndRequestBluetoothPermissions()) {
                if (bluetoothService.connectToBluetoothDevice(it)) {
                    bluetoothService.writeDeviceData("ready")
                    dismiss() // Close the dialog
                    startActivity(Intent(it, ROMExerciseActivity::class.java))
                }
            } else {
                Toast.makeText(it, "Bluetooth permissions disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
