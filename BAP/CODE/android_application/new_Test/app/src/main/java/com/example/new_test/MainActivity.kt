package com.example.new_test

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var deviceListAdapter: DeviceListAdapter

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestBluetoothPermissions()  //BLE permissions moeten tijdens runtime gevraagd worden
    }

    // Vraag de Bluetooth-permissies aan voor Android 12+ (API 31)
    private fun requestBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Als de permissie niet is verleend, vraag deze aan
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Als de permissie al is verleend, kun je beginnen met scannen
            startScanning()
        }
    }

    // Controleer de permissies voor oudere versies (Android 6.0 en hoger)
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Als de permissie niet is verleend, vraag deze aan
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Als de permissie al is verleend, kun je beginnen met scannen
            startScanning()
        }
    }

    // Callback voor de resultaten van de permissie-aanvraag
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Als de permissie is verleend, start het scannen
                startScanning()
            } else {
                // Als de permissie wordt geweigerd, toon een foutmelding
                Toast.makeText(this, "Bluetooth scanning permissie is vereist.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Start het scannen naar BLE-apparaten
    @SuppressLint("MissingPermission")
    private fun startScanning() {
        // Initialiseer Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is niet ingeschakeld", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val recyclerView: RecyclerView = findViewById(R.id.deviceList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Maak de adapter met een lege lijst en voeg de klikfunctionaliteit toe
        deviceListAdapter = DeviceListAdapter(emptyList()) { device ->
            connectToDevice(device)  // Start de verbinding met het geselecteerde apparaat
        }
        recyclerView.adapter = deviceListAdapter

        bluetoothLeScanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // Maak verbinding met het geselecteerde apparaat via GATT
        val gatt = device.connectGatt(this, false, gattCallback)

        // Geef feedback over de verbinding
        Toast.makeText(this, "Verbinding maken met ${device.name}", Toast.LENGTH_SHORT).show()

        // Zodra de verbinding is gemaakt, navigeer naar het nieuwe scherm en geef het BluetoothDevice door
        val intent = Intent(this, GattDetailsActivity::class.java)
        intent.putExtra("EXTRA_DEVICE", device)  // BluetoothDevice doorgeven via de Intent
        startActivity(intent)
    }


    // Callback voor de GATT-verbinding
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Verbonden met ESP32", Toast.LENGTH_SHORT).show()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Verbinding verbroken", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Verbinding is tot stand gebracht en services zijn ontdekt
                // Je kunt nu communiceren met het apparaat via GATT
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Services ontdekt", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Callback voor het scannen naar apparaten
    private val scanCallback = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                // Verkrijg de huidige lijst van apparaten uit de adapter
                val currentList = deviceListAdapter.getDevices().toMutableList()

                // Voeg het nieuwe apparaat toe aan de lijst
                if (!currentList.contains(device)) { // Controleer of het apparaat al in de lijst staat
                    currentList.add(device)
                }

                // Werk de adapter bij met de nieuwe lijst
                deviceListAdapter.updateDeviceList(currentList)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "Scan mislukt: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }
}
