package com.example.adaptanklebrace

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.adaptanklebrace.services.BluetoothService
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences

    private var settingsActivity = SettingsActivity()
    private lateinit var bluetoothService: BluetoothService
    private var isBluetoothServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothService.LocalBinder).getService()
            isBluetoothServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBluetoothServiceBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the app theme
        // Check saved preference for night mode
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isNightMode = settingsActivity.getPreference(sharedPreferences, "nightMode", false)
        settingsActivity.changeAppTheme(isNightMode)
        setContentView(R.layout.activity_main)

        // Set up Toolbar
        val toolbar: Toolbar = findViewById(R.id.homeToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24) // Icon for sidebar

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)

        // Initialize NavigationView
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_common_exercises -> startActivity(Intent(this, CommonExercisesActivity::class.java))
                R.id.nav_recovery_plan -> startActivity(Intent(this, RecoveryPlanActivity::class.java))
                R.id.nav_recovery_progress -> startActivity(Intent(this, RecoveryProgressActivity::class.java))
                R.id.nav_notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
            }
            drawerLayout.closeDrawers() // Close the sidebar
            true
        }

        // Start the Bluetooth service
        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Start exercise from button (triggers connection to device)
        val startExerciseButton: Button = findViewById(R.id.startExerciseBtn)
        startExerciseButton.setOnClickListener {
            val connectDeviceFragment = ConnectDeviceFragment()
            connectDeviceFragment.show(supportFragmentManager, "connect_device")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Toggle sidebar state
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START) // Close if it's open
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)  // Open if it's closed
                }
                true
            }
            R.id.action_settings -> {
                // Open settings activity
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_toolbar_menu, menu)
        return true
    }

    override fun onDestroy() {
        if (isBluetoothServiceBound) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }




    /*** BLUETOOTH INITIALIZATION  ***/
//    private var bluetoothAdapter: BluetoothAdapter? = null
//    private var bluetoothGatt: BluetoothGatt? = null
//    private var bluetoothDevice: BluetoothDevice? = null

    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 2
//    private val bluetoothService = BluetoothService()
//    private val serviceUUID: UUID = UUID.fromString("f3b4f9a8-25b8-4ee1-8b69-0a61a964de15")
//    private val characteristicUUID: UUID = UUID.fromString("f8c2f5f0-4e8c-4a95-b9c1-3c8c33b457c3")

//    private val _deviceLiveData = MutableLiveData<Int>()
//    val deviceLiveData: LiveData<Int> get() = _deviceLiveData

    @RequiresApi(Build.VERSION_CODES.S)
    fun checkAndRequestBluetoothPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                BLUETOOTH_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothService.initBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    fun initBluetooth() {
//        val bluetoothManager = getSystemService(BluetoothManager::class.java)
//        bluetoothAdapter = bluetoothManager.adapter
//
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
//            finish()
//        }
//    }

//    @SuppressLint("MissingPermission")
//    fun connectToBluetoothDevice(): Boolean {
//        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//        bluetoothDevice = pairedDevices?.firstOrNull { it.name == "ADAPT" }
//
//        if (bluetoothDevice == null) {
//            Toast.makeText(this, "A.D.A.P.T. device not found", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        // Connect to the GATT server
//        bluetoothGatt = bluetoothDevice?.connectGatt(this, false, object : BluetoothGattCallback() {
//            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
//                    Log.i("Bluetooth", "Connected to GATT server.")
//                    gatt.discoverServices() // Discover services after successful connection
//                } else {
//                    Log.w("Bluetooth", "Connection failed: $status")
//                }
//            }
//
//            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    Log.i("Bluetooth", "Services discovered")
//                    val characteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//                    characteristic?.let {
//                        enableNotifications(gatt, it)
//                        //readDeviceCharacteristic()
//                    }
//                } else {
//                    Log.w("Bluetooth", "Service discovery failed: $status")
//                }
//            }
//
//            override fun onCharacteristicRead(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int
//            ) {
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    val value = characteristic.value
//                    Log.i("Bluetooth", "Characteristic read: ${value?.contentToString()}")
//                }
//            }
//
//            override fun onCharacteristicWrite(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int
//            ) {
//                super.onCharacteristicWrite(gatt, characteristic, status)
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    Log.i("Bluetooth", "Successfully wrote to characteristic: ${characteristic.uuid}")
//                } else {
//                    Log.w("Bluetooth", "Failed to write to characteristic: ${characteristic.uuid}, status: $status")
//                }
//            }
//
//            override fun onCharacteristicChanged(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic
//            ) {
//                // Handle notifications from the device
//                val value = characteristic.value?.firstOrNull()?.toInt() ?: 0
//                Log.i("Bluetooth", "Notification received: $value")
//                _deviceLiveData.postValue(value)
//            }
//        })
//        return true
//    }

//    fun readDeviceData() {
//        bluetoothGatt?.let { gatt ->
//            val characteristic = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            characteristic?.let {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                    gatt.readCharacteristic(it)
//                    // This callback will trigger when the characteristic is read
//                    gatt.setCharacteristicNotification(it, true)
//                } else {
//                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
//                }
//            }
//        }
//    }

//    @Suppress("DEPRECATION")
//    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//            gatt.setCharacteristicNotification(characteristic, true)
//
//            // Retrieve the descriptor for enabling notifications
//            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//            if (descriptor != null) {
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                val status = gatt.writeDescriptor(descriptor)
//                if (status) {
//                    Log.i("Bluetooth", "Descriptor write successful")
//                } else {
//                    Log.e("Bluetooth", "Failed to write descriptor for notifications")
//                }
//            } else {
//                Log.w("Bluetooth", "Descriptor for notifications not found")
//            }
//        } else {
//            Log.w("Bluetooth", "Bluetooth connect permission not granted.")
//        }
//    }

//    @Suppress("DEPRECATION")
//    fun writeDeviceData(data: String) {
//        bluetoothGatt?.let { gatt ->
//            val characteristic: BluetoothGattCharacteristic? = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            characteristic?.let {
//                it.value = data.toByteArray()
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                    val status = gatt.writeCharacteristic(it)
//                    if (status) {
//                        Log.i("Bluetooth", "Data sent: $data")
//                    } else {
//                        Log.w("Bluetooth", "Failed to write data: $data")
//                    }
//                } else {
//                    Log.w("Bluetooth", "Bluetooth connect permission not granted.")
//                }
//            }
//        }
//    }
}
