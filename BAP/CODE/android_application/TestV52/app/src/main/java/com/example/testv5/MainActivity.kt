package com.example.bleesp32

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.testv5.R

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null

    private val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b" // Vervang door jouw service UUID
    private val CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8" // Vervang door jouw kenmerk UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verzoek om Bluetooth-permissies voor Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            startBleScan()
        }

        findViewById<Button>(R.id.scanButton).setOnClickListener {
            startBleScan()
        }
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            startBleScan()
        } else {
            Toast.makeText(this, "BLE-permissies vereist", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            if (device.name == "ESP32") { // Filter op ESP32 naam
                bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "Scan mislukt met foutcode: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBleScan() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        } else {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                val characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                gatt.readCharacteristic(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value.toString(Charsets.UTF_8)
                runOnUiThread {
                    findViewById<TextView>(R.id.dataTextView).text = data
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}
