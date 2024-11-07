package com.example.testv2

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.ListView
import androidx.activity.ComponentActivity
import android.Manifest
import android.bluetooth.le.ScanResult
import androidx.core.app.ActivityCompat
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                REQUEST_ENABLE_BT
            )
        }
        val deviceListView = findViewById<ListView>(R.id.deviceListView)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.d("Bluetooth", "Device doesn't support Bluetooth")
        } else if (bluetoothAdapter.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        setupListView()
        startScan()
    }

    private val leScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device: BluetoothDevice = result.device
            val deviceName = device.name ?: "Onbekend apparaat"
            val deviceAddress = device.address
            addDeviceToList(deviceName, deviceAddress)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            results.forEach { result ->
                val device = result.device
                val deviceName = device.name ?: "Onbekend apparaat"
                val deviceAddress = device.address
                addDeviceToList(deviceName, deviceAddress)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(
                this@MainActivity,
                "Scan mislukt met foutcode: $errorCode",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startScan() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.startScan(leScanCallback)
        Toast.makeText(this, "Scan gestart", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({
            stopScan()
        }, 10000)
    }

    private fun stopScan() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.stopScan(leScanCallback)
        Toast.makeText(this, "Scan gestopt", Toast.LENGTH_SHORT).show()
    }

    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val deviceList = mutableListOf<String>()

    private fun setupListView() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        val listView: ListView = findViewById(R.id.deviceListView)
        listView.adapter = deviceListAdapter
    }

    private fun addDeviceToList(deviceName: String, deviceAddress: String) {
        val deviceInfo = "$deviceName - $deviceAddress"
        if (!deviceList.contains(deviceInfo)) {
            deviceList.add(deviceInfo)
            deviceListAdapter.notifyDataSetChanged()  // Update de ListView
        }
    }


}
