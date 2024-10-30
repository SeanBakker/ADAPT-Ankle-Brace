package com.example.adaptanklebrace

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24) // Icon for sidebar

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)

        // Initialize NavigationView
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_recovery_plan -> {
                    // Start Recovery Plan Activity
                    startActivity(Intent(this, RecoveryPlanActivity::class.java))
                }
                R.id.nav_recovery_progress -> {
                    // Start Recovery Progress Activity
                    startActivity(Intent(this, RecoveryProgressActivity::class.java))
                }
                R.id.nav_insights -> {
                    // Start Insights Activity
                    startActivity(Intent(this, InsightsActivity::class.java))
                }
            }
            drawerLayout.closeDrawers() // Close the sidebar
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START) // Open sidebar when home icon is clicked
                true
            }
            R.id.action_settings -> {
                // Start Settings Activity
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




    /*** BLUETOOTH INITIALIZATION  ***/

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 2

    // Define the service and characteristic UUIDs
    private val serviceUUID: UUID = UUID.fromString("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15") // Change to your service UUID
    private val characteristicUUID: UUID = UUID.fromString("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3") // Change to your characteristic UUID

//    @RequiresApi(Build.VERSION_CODES.S)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // Check and request permissions
//        if (checkAndRequestBluetoothPermissions()) {
//            initBluetooth()
//        }
//
//        val connectBtn: Button = findViewById(R.id.connectBtn)
//        connectBtn.setOnClickListener {
//            connectToBluetoothDevice()
//        }
//
//        val sendBtn: Button = findViewById(R.id.sendDataBtn)
//        sendBtn.setOnClickListener {
//            val inputField: EditText = findViewById(R.id.inputField)
//            val dataToSend = inputField.text.toString()
//            writeData(dataToSend)
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAndRequestBluetoothPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                BLUETOOTH_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initBluetooth() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToBluetoothDevice() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        bluetoothDevice = pairedDevices?.firstOrNull { it.name == "Nano33BLE" }

        if (bluetoothDevice == null) {
            Toast.makeText(this, "Nano33BLE device not found", Toast.LENGTH_SHORT).show()
            return
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
                        readCharacteristic(gatt, it) // Optional: read the characteristic after enabling notifications
                    }
                } else {
                    Log.w("Bluetooth", "Service discovery failed: $status")
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = characteristic.value
                    Log.i("Bluetooth", "Characteristic read: ${value?.contentToString()}")
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                // Handle notifications from the device
                val value = characteristic.value
                Log.i("Bluetooth", "Notification received: ${value?.contentToString()}")
            }
        })
    }

    private fun readCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt.readCharacteristic(characteristic)
        } else {
            Log.w("Bluetooth", "Bluetooth connect permission not granted.")
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt.setCharacteristicNotification(characteristic, true)
            // Configure descriptor for notifications
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            Log.w("Bluetooth", "Bluetooth connect permission not granted.")
        }
    }

    private fun writeData(data: String) {
        bluetoothGatt?.let { gatt ->
            val characteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
            characteristic?.let {
                it.value = data.toByteArray() // Convert string data to byte array
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val status = gatt.writeCharacteristic(it)
                    if (status) {
                        Log.i("Bluetooth", "Data sent: $data")
                    } else {
                        Log.w("Bluetooth", "Failed to write data")
                    }
                } else {
                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
                }
            }
        }
    }
}
