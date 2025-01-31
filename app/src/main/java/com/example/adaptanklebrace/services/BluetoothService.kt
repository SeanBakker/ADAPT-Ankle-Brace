package com.example.adaptanklebrace.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.utils.ExerciseUtil
import java.util.UUID

class BluetoothService : Service() {

    /*** BLUETOOTH INITIALIZATION  ***/
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private val serviceUUID: UUID = UUID.fromString("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15")
    private val readCharacteristicUUID: UUID = UUID.fromString("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3")
    private val writeCharacteristicUUID: UUID = UUID.fromString("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c4")
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1002

    val _deviceLiveData = MutableLiveData<Pair<Int, Int>?>()
    val deviceLiveData: LiveData<Pair<Int, Int>?> get() = _deviceLiveData

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
            ExerciseUtil.showToast(applicationContext, LayoutInflater.from(applicationContext), "Bluetooth not supported")
            stopSelf()  // Stop the service if Bluetooth is not supported
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToBluetoothDevice(context: Context): Boolean {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        bluetoothDevice = pairedDevices?.firstOrNull { it.name == "ADAPT" } // Sometimes named: Arduino

        if (bluetoothDevice == null) {
            ExerciseUtil.showToast(context, LayoutInflater.from(context), "A.D.A.P.T. device not found")
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
                    val readCharacteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(readCharacteristicUUID)
                    readCharacteristic?.let {
                        enableNotifications(gatt, it)
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
                    val value = characteristic.value
                    if (value.isNotEmpty()) {
                        if (value.size == 1) {
                            // Single integer
                            val singleValue = value[0].toInt() and 0xFF
                            _deviceLiveData.postValue(Pair(singleValue, singleValue)) // Store as a pair for consistency
                            Log.i("Bluetooth", "Characteristic read (single): $singleValue")
                        } else if (value.size >= 2) {
                            // Pair of integers
                            val firstValue = value[0].toInt() and 0xFF
                            val secondValue = value[1].toInt() and 0xFF
                            _deviceLiveData.postValue(Pair(firstValue, secondValue))
                            Log.i("Bluetooth", "Characteristic read (pair): ($firstValue, $secondValue)")
                        }
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
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val value = characteristic.value
                if (value.isNotEmpty()) {
                    if (value.size == 1) {
                        // Single integer
                        val singleValue = value[0].toInt() and 0xFF
                        _deviceLiveData.postValue(Pair(singleValue, singleValue)) // Store as a pair for consistency
                        Log.i("Bluetooth", "Characteristic changed (single): $singleValue")
                    } else if (value.size >= 2) {
                        // Pair of integers
                        val firstValue = value[0].toInt() and 0xFF
                        val secondValue = value[1].toInt() and 0xFF
                        _deviceLiveData.postValue(Pair(firstValue, secondValue))
                        Log.i("Bluetooth", "Characteristic changed (pair): ($firstValue, $secondValue)")
                    }
                } else {
                    Log.w("Bluetooth", "Characteristic changed: no data received")
                }
            }
        })
        return true
    }

    fun checkAndRequestBluetoothPermissions(activity: Activity): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                BLUETOOTH_PERMISSION_REQUEST_CODE)
            return false
        }
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
            val readCharacteristic = gatt.getService(serviceUUID)?.getCharacteristic(readCharacteristicUUID)
            readCharacteristic?.let {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Read characteristic to get initial value
                    gatt.readCharacteristic(it)
                } else {
                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun writeDeviceData(data: String) {
        bluetoothGatt?.let { gatt ->
            val writeCharacteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(writeCharacteristicUUID)
            writeCharacteristic?.let {
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

    fun resetLiveData() {
        _deviceLiveData.postValue(null)
    }

    fun disconnect() {
        if (bluetoothGatt != null) {
            Log.i("Bluetooth", "Disconnecting from Bluetooth device.")
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                resetLiveData()
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        } else {
            Log.w("Bluetooth", "No active Bluetooth connection to disconnect.")
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundServiceWithNotification() {
        val notificationChannelId = "BluetoothServiceChannel"
        val channel = NotificationChannel(
            notificationChannelId,
            "Bluetooth Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

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
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }
        bluetoothGatt?.close() // Close Bluetooth connection when service is destroyed
    }
}
