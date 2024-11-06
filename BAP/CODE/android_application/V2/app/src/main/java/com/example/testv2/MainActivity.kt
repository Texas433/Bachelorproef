package com.example.testv2

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Handle scan result here
            val device: BluetoothDevice? = result.device
            // ... process the device
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            // Handle batch scan results here
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // Handle scan failure here
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            // Handle the case
        } else {
            // Bluetooth is available, use bluetoothAdapter
            val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }
    private val bluetoothLeScanner =  BluetoothAdapter.bluetoothLeScanner
    private val leDeviceListAdapter = LeDeviceListAdapter()
    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private fun scanLeDevice() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }

// Device scan callback.
        private val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                leDeviceListAdapter.addDevice(result.device)
                leDeviceListAdapter.notifyDataSetChanged()
            }
        }
    }
}