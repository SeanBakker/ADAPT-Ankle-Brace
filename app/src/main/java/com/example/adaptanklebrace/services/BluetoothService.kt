package com.example.adaptanklebrace.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.adaptanklebrace.R
import java.util.UUID

class BluetoothService : Service() {

    /*** BLUETOOTH INITIALIZATION  ***/
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private val serviceUUID: UUID = UUID.fromString("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15")
    private val characteristicUUID: UUID = UUID.fromString("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3")

    private val _deviceLiveData = MutableLiveData<Int>()
    val deviceLiveData: LiveData<Int> get() = _deviceLiveData

    companion object {
        var instance: BluetoothService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initBluetooth()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as a foreground service
        startForegroundServiceWithNotification()
        return START_STICKY
    }

    fun initBluetooth() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            stopSelf()  // Stop the service if Bluetooth is not supported
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToBluetoothDevice(context: Context): Boolean {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        bluetoothDevice = pairedDevices?.firstOrNull { it.name == "ADAPT" } // Sometimes named: Arduino

        if (bluetoothDevice == null) {
            Toast.makeText(context, "A.D.A.P.T. device not found", Toast.LENGTH_SHORT).show()
            return false
        }

        // Connect to the GATT server
        bluetoothGatt = bluetoothDevice?.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i("Bluetooth", "Connected to GATT server.")
                    gatt.discoverServices() // Discover services after successful connection
                } else {
                    Log.w("Bluetooth", "Connection failed: $status")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("Bluetooth", "Services discovered")
                    val characteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
                    characteristic?.let {
                        enableNotifications(gatt, it)
                        writeDeviceData("ready")
                    }
                } else {
                    Log.w("Bluetooth", "Service discovery failed: $status")
                }
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val byteArray = characteristic.value
                    if (byteArray.isNotEmpty()) {
                        // Convert the first byte to an integer
                        val value = byteArray[0].toInt() and 0xFF // Convert to unsigned value
                        _deviceLiveData.postValue(value)
                        Log.i("Bluetooth", "Characteristic read: $value°")
                    } else {
                        Log.w("Bluetooth", "Characteristic read: no data received")
                    }
                } else {
                    Log.e("Bluetooth", "Failed to read characteristic, status: $status")
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("Bluetooth", "Successfully wrote to characteristic: ${characteristic.uuid}")
                } else {
                    Log.w("Bluetooth", "Failed to write to characteristic: ${characteristic.uuid}, status: $status")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                // Handle notifications from the device
                val value = characteristic.value?.firstOrNull()?.toInt() ?: 0
                Log.i("Bluetooth", "Notification received: $value")
                _deviceLiveData.postValue(value)
            }
        })
        return true
    }

    @Suppress("DEPRECATION")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gatt.setCharacteristicNotification(characteristic, true)

            // Retrieve the descriptor for enabling notifications
            val descriptor =
                characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val status = gatt.writeDescriptor(descriptor)
                if (status) {
                    Log.i("Bluetooth", "Descriptor write successful")
                } else {
                    Log.e("Bluetooth", "Failed to write descriptor for notifications")
                }
            }
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            }
        } else {
            Log.w("Bluetooth", "Bluetooth connect permission not granted.")
        }
    }

    fun readDeviceData() {
        bluetoothGatt?.let { gatt ->
            val characteristic = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
            characteristic?.let {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.readCharacteristic(it)
                    // This callback will trigger when the characteristic is read
                    gatt.setCharacteristicNotification(it, true)
                } else {
                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun writeDeviceData(data: String) {
        bluetoothGatt?.let { gatt ->
            val characteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
            characteristic?.let {
                it.value = data.toByteArray()
                it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val status = gatt.writeCharacteristic(it)
                    if (status) {
                        Log.i("Bluetooth", "Data sent: $data")
                    } else {
                        Log.e("Bluetooth", "Failed to write data: $data")
                    }
                } else {
                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
                }
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundServiceWithNotification() {
        val notificationChannelId = "BluetoothServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Bluetooth Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Bluetooth Service")
            .setContentText("Bluetooth connection is active")
            .setSmallIcon(R.drawable.baseline_bluetooth_searching_24)
            .build()

        startForeground(1, notification)  // ID 1 is used for the notification
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    // Bind the service to interact with the Bluetooth functionality
    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }
        bluetoothGatt?.close() // Close Bluetooth connection when service is destroyed
    }
}
