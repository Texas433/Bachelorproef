package com.example.new_test

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.content.Context
import android.util.Log

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Stel de Toolbar in
        val toolbar = findViewById<Toolbar>(R.id.toolbar3)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val serverUrlEditText = findViewById<EditText>(R.id.server_url)
        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginbutton)
// ✅ Server-URL opslaan


        loginButton.setOnClickListener {
            val serverUrl = serverUrlEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("server_url", serverUrl)
                apply()
            }
            val savedUrl = sharedPref.getString("server_url", null)
            val savedToken = sharedPref.getString("auth_token", null)

            Log.d("SettingsActivity", "✅ Opgeslagen Server URL: $savedUrl")
            Log.d("SettingsActivity", "✅ Opgeslagen Token: $savedToken")
            if (serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                thread {
                    postLogin(serverUrl, username, password)
                }
            } else {
                Toast.makeText(this, "Vul alle velden in", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun postLogin(url: String, username: String, password: String) {
        val loginUrl = "$url/login"
        Log.d("SettingsActivity", "✅ Login Server URL: $loginUrl")
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        try {
            Log.d("SettingsActivity", "➡️ Verbinding openen")
            val connection = URL(loginUrl).openConnection() as HttpURLConnection

            Log.d("SettingsActivity", "➡️ Request configureren")
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("ngrok-skip-browser-warning", "true")
            connection.doOutput = true

            Log.d("SettingsActivity", "➡️ JSON versturen")
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(json.toString().toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            Log.d("SettingsActivity", "➡️ Responscode lezen")
            val responseCode = connection.responseCode

            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            val jsonResponse = JSONObject(responseMessage)
            val token = jsonResponse.optString("token", null)

            runOnUiThread {
                if (token != null) {
                    // ✅ Token opslaan in SharedPreferences
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("auth_token", token)
                        apply()
                    }

                    Toast.makeText(this, "Token opgeslagen!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Geen token ontvangen!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Fout: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
