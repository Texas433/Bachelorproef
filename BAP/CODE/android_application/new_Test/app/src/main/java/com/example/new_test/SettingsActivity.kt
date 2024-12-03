package com.example.new_test

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Stel de Toolbar in
        val toolbar = findViewById<Toolbar>(R.id.toolbar3)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"


        val serverUrlEditText = findViewById<EditText>(R.id.server_url)
        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginbutton)
        loginButton.setOnClickListener {
            val serverUrl = serverUrlEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (serverUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                simulatePostRequest(serverUrl, username, password)
            } else {
                Toast.makeText(this, "Vul alle velden in", Toast.LENGTH_SHORT).show()
            }
        }


    }
    private fun simulatePostRequest(url: String, username: String, password: String) {
        Toast.makeText(this, "POST naar $url\nUsername: $username\nPassword: $password", Toast.LENGTH_LONG).show()
     //netwerk req invoegen
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
