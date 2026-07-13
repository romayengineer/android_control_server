package com.romayengineer.wifimouse

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.romayengineer.wifimouse.service.WiFiMouseService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val portEditText = findViewById<EditText>(R.id.port_input)
        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val statusText = findViewById<TextView>(R.id.status_text)

        portEditText.setText(WiFiMouseService.DEFAULT_PORT.toString())

        startButton.setOnClickListener {
            val port = portEditText.text.toString().toIntOrNull() ?: WiFiMouseService.DEFAULT_PORT
            val serviceIntent = Intent(this, WiFiMouseService::class.java)
            serviceIntent.putExtra(WiFiMouseService.KEY_PORT, port)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }

            statusText.text = "Server started on port $port"
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, WiFiMouseService::class.java)
            stopService(serviceIntent)
            statusText.text = "Server stopped"
        }
    }
}
