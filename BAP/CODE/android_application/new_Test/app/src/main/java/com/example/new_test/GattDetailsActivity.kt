package com.example.new_test

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import java.util.UUID
import android.content.Intent
import android.widget.Switch
import android.content.Context
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GattDetailsActivity : AppCompatActivity() {

    private var gatt: BluetoothGatt? = null
    private lateinit var characteristicDataTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_details)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Gatt Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Toon de terug-pijl in de Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Koppel het menu aan de Toolbar
        menuInflater.inflate(R.menu.menu_connected, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_toggle_web -> {
                // Toggle Web Transmission actie
                Toast.makeText(this, "Web Transmission Toggled", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_settings -> {
                // Ga naar het instellingen scherm
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            android.R.id.home -> {
                // Behandel de terug-pijl
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    // GATT Callback om de verbinding te beheren
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // Wanneer de verbinding succesvol is, services ontdekken
                gatt?.discoverServices()
                gatt?.requestMtu(256)  // vraag om een MTU van 256 bytes
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "Verbinding verbroken", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GattDetailsActivity", "MTU succesvol gewijzigd naar: $mtu bytes")
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "MTU ingesteld op $mtu bytes", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("GattDetailsActivity", "MTU wijziging mislukt")
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "MTU wijziging mislukt", Toast.LENGTH_SHORT).show()
                }
            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d("GattDetailsActivity", "onServicesDiscovered: status=$status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
                val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

                val service = gatt?.getService(serviceUUID)
                val characteristic = service?.getCharacteristic(characteristicUUID)

                if (characteristic != null) {
                    enableNotifications(characteristic)
                    Log.d("GattDetailsActivity", "Kenmerk gevonden: ${characteristic.uuid}")
                     readGattCharacteristic(characteristic)
                } else {
                    Log.d("GattDetailsActivity", "Kenmerk niet gevonden voor UUID: $characteristicUUID")
                    runOnUiThread {
                        Toast.makeText(this@GattDetailsActivity, "Kenmerk niet gevonden", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "Services niet ontdekt: $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
        fun uploadSensorData(token: String, encryptedData: String) {
            // ✅ Server-URL ophalen
            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val serverUrl = sharedPref.getString("server_url", null)

            if (serverUrl == null) {
                Log.e("GattDetailsActivity", "Geen server-URL ingesteld!")
                return
            }

            val urlString = serverUrl.trimEnd('/') + "/upload"
            val url = URL(urlString)

            val jsonInputString = """{"data":"$encryptedData"}"""

            Thread {
                try {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    Log.d("GattDetailsActivity", "trying to upload")
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    Log.d("GattDetailsActivity", "token: $token")
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.doOutput = true

                    val outputStream = DataOutputStream(connection.outputStream)
                    outputStream.writeBytes(jsonInputString)
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = connection.responseCode
                    val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                    if (responseCode == 200) {
                        Log.d("GattDetailsActivity", "Upload geslaagd: $responseMessage")
                    } else {
                        Log.e("GattDetailsActivity", "Fout bij upload: $responseCode - $responseMessage")
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            Log.d("GattDetailsActivity", "onCharacteristicRead: status=$status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic?.value
                if (value != null && value.isNotEmpty()) {
                    val result = String(value, Charsets.UTF_8)
                    Log.d("GattDetailsActivity", "Gelezen data: $result")
                    runOnUiThread {
                        characteristicDataTextView.text = "Gelezen data: $result"
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GattDetailsActivity, "Leeg of geen waarde ontvangen", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@GattDetailsActivity, "Fout bij het lezen van het kenmerk", Toast.LENGTH_SHORT).show()
                }
            }
        }


        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic != null) {
                val value = characteristic.value
                val result = value.joinToString(" ") { byte -> "%02X".format(byte) }
                Log.d("GattDetailsActivity", "Notificatie ontvangen: $result")

                // ✅ Token ophalen uit SharedPreferences
                val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPref.getString("auth_token", null)
                if (token != null) {
                    // 🚀 Upload de ontvangen data
                    uploadSensorData(token, result)
                } else {
                    Log.e("GattDetailsActivity", "Geen token gevonden! Gebruiker is niet ingelogd.")
                }

                // Update de UI met de nieuwe waarde
                runOnUiThread {
                    characteristicDataTextView.text = "received: $result"
                }
            }
        }

    }

    // Functie om notificaties in te schakelen voor het kenmerk
    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val gatt = gatt
        if (gatt != null) {
            // Schakel notificaties in voor het kenmerk
            gatt.setCharacteristicNotification(characteristic, true)

            // Haal de descriptor op voor de Client Characteristic Configuration Descriptor (CCCD)
            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )

            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
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

    // Functie die door een knop in de UI wordt aangeroepen om het kenmerk opnieuw te lezen
    fun readCharacteristicAgain(view: View) {
        val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            // Lees het kenmerk opnieuw
            readGattCharacteristic(characteristic)
        } else {
            Toast.makeText(this, "Kenmerk niet gevonden", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendDataToServer() {
        if (isWebTransmissionEnabled()) {
            Toast.makeText(this, "Data verzonden naar server", Toast.LENGTH_SHORT).show()
            // Voeg hier een echte POST-request toe met Retrofit of OkHttp
        }
    }

    private fun isWebTransmissionEnabled(): Boolean {
        val switchItem = findViewById<Switch>(R.id.switch_toggle_web)
        return switchItem?.isChecked == true
    }
    private fun fetchDataFromServer() {
        Toast.makeText(this, "Data opgehaald van server", Toast.LENGTH_SHORT).show()
        // Voeg hier een echte GET-request toe met Retrofit of OkHttp
    }


}
