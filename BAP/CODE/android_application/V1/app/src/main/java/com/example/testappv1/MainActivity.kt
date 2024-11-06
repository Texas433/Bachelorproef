package com.example.testappv1

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.testappv1.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dataTextView: TextView
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataTextView = findViewById(R.id.dataTextView)
        val deviceListView = findViewById<ListView>(R.id.deviceListView)

        // Initialiseer de adapter voor de apparaatlijst

        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView.adapter = deviceAdapter
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        } else if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        // Bluetooth-initialisatie
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Start het scannen als de permissies correct zijn
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        // Stel de clicklistener in om verbinding te maken met het geselecteerde apparaat
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = deviceList[position]
            if (selectedDevice.name == "ESP32") { // Controleer of het de ESP32 is
                connectToDevice(selectedDevice)
            } else {
                Toast.makeText(this, "Selecteer de ESP32 om verbinding te maken", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScanning() {
        val scanSettings = android.bluetooth.le.ScanSettings.Builder().build()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner != null) {
            scanner.startScan(null, scanSettings, scanCallback)
        } else {
            dataTextView.text = "Bluetooth LE Scanner niet beschikbaar"
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            if (device != null && !deviceList.contains(device)) {
                // Voeg het apparaat toe aan de lijst als het nog niet is toegevoegd
                deviceList.add(device)
                deviceAdapter.add("${device.name ?: "Onbekend apparaat"} - ${device.address}")
                deviceAdapter.notifyDataSetChanged()
            }
            device?.let {
                deviceList.add(it.name + "\n" + it.address)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt = device.connectGatt(this, false, gattCallback)
            dataTextView.text = "Verbinden met ${device.name ?: "Onbekend apparaat"}..."
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    dataTextView.text = "Verbonden met ${gatt.device.name}"
                }
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handler.post(runnable)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                val dataString = data?.let { String(it) } ?: "Geen data ontvangen"
                updateUI(dataString)
            } else {
                runOnUiThread {
                    dataTextView.text = "Fout bij lezen van gegevens"
                }
            }
        }
    }

    private fun readData(gatt: BluetoothGatt) {
        val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val characteristic = gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.readCharacteristic(characteristic)
            }
        } else {
            runOnUiThread {
                dataTextView.text = "Kan geen gegevens lezen"
            }
        }
    }

    private fun updateUI(dataString: String) {
        runOnUiThread {
            dataTextView.text = dataString
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            gatt?.let { readData(it) }
            handler.postDelayed(this, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gatt?.close()
        gatt = null
    }

    // Voeg onRequestPermissionsResult toe zoals eerder besproken
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            dataTextView.text = "Bluetooth-toestemming geweigerd."
        }
    }
}
