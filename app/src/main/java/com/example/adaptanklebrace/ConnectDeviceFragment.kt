package com.example.adaptanklebrace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment

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
            // Check and request bluetooth permissions to ADAPT device
            if (it.checkAndRequestBluetoothPermissions()) {
                it.initBluetooth()
            }

            val isConnected = it.connectToBluetoothDevice()

            if (isConnected) {
                dismiss() // Close the dialog
                startActivity(Intent(context, ROMExerciseActivity::class.java))
            }
        }
    }
}
