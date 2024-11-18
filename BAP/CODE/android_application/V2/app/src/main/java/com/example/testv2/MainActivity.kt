package com.example.testv2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import no.nordicsemi.android.ble
import no.nordicsemi.

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: MyBleManager
    private lateinit var dataTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton: Button = findViewById(R.id.connectButton)
        dataTextView = findViewById(R.id.dataTextView)

        // Initialiseer BLE-manager
        bleManager = MyBleManager(this)

        // Stel de callback in voor ontvangen data
        bleManager.onDataReceived = { data ->
            runOnUiThread {
                dataTextView.text = "Data: $data"
            }
        }

        // Verbind met het apparaat wanneer op de knop wordt gedrukt
        connectButton.setOnClickListener {
            connectToDevice()
        }
    }

    private fun connectToDevice() {
        // Zoek en verbind met het gewenste apparaat (ESP32) hier
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice("MAC_ADDRESS_HERE") // Vul het MAC-adres in

        if (device != null) {
            bleManager.connect(device)
                .retry(3, 100)  // Optioneel: opnieuw proberen bij verbindingsproblemen
                .enqueue()
        } else {
            dataTextView.text = "Apparaat niet gevonden"
        }
    }
}
