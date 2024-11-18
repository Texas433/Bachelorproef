package com.example.new_test

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class GattDetailsActivity : AppCompatActivity() {

    private var gatt: BluetoothGatt? = null
    private lateinit var characteristicDataTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_details)

        // Initialiseer de TextView voor het tonen van de gelezen gegevens
        characteristicDataTextView = findViewById(R.id.characteristicData)

        // Haal de BluetoothDevice op via de Intent
        val device = intent.getParcelableExtra<BluetoothDevice>("EXTRA_DEVICE")

        if (device != null) {
            // Maak verbinding met het apparaat
            gatt = device.connectGatt(this, false, gattCallback)
        } else {
            Toast.makeText(this, "Geen apparaat om mee te verbinden.", Toast.LENGTH_SHORT).show()
        }
    }

    // GATT Callback om de verbinding te beheren
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // Wanneer de verbinding succesvol is, services ontdekken
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "Verbinding verbroken", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Zodra de services zijn ontdekt, lees het kenmerk
                val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
                val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

                val service = gatt?.getService(serviceUUID)
                val characteristic = service?.getCharacteristic(characteristicUUID)

                if (characteristic != null) {
                    readGattCharacteristic(characteristic)
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GattDetailsActivity, "Kenmerk niet gevonden", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic?.value
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "Gelezen data: ${value?.contentToString()}", Toast.LENGTH_SHORT).show()
                    characteristicDataTextView.text = "Gelezen data: ${value?.contentToString()}"
                }
            }
        }
    }

    // Functie voor het lezen van GATT-kenmerken
    private fun readGattCharacteristic(characteristic: BluetoothGattCharacteristic) {
        gatt?.readCharacteristic(characteristic)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Sluit de GATT-verbinding wanneer de activiteit wordt vernietigd
        gatt?.close()
        gatt = null
    }

    // Functie voor het opnieuw lezen van het kenmerk
    fun readCharacteristicAgain(view: View) {
        // Haal de karakteristiek opnieuw op en lees het
        val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            // Lees het kenmerk opnieuw
            readGattCharacteristic(characteristic)
            val value = characteristic?.value
            characteristicDataTextView.text = "Gelezen data: ${value?.contentToString()}"
        } else {
            Toast.makeText(this, "Kenmerk niet gevonden", Toast.LENGTH_SHORT).show()
        }
    }
}
