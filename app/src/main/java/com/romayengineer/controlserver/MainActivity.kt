package com.romayengineer.controlserver

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.romayengineer.controlserver.service.WiFiMouseService
import com.romayengineer.controlserver.ui.CursorView
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    companion object {
        private var cursorView: CursorView? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateCursorPosition(x: Int, y: Int) {
            mainHandler.post {
                cursorView?.updateCursorPosition(x, y)
            }
        }

        fun setCursorVisible(visible: Boolean) {
            mainHandler.post {
                cursorView?.setCursorVisible(visible)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val portEditText = findViewById<EditText>(R.id.port_input)
        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val statusText = findViewById<TextView>(R.id.status_text)
        val ipAddressText = findViewById<TextView>(R.id.ip_address_text)

        cursorView = findViewById<CursorView>(R.id.cursor_view)

        portEditText.setText(WiFiMouseService.DEFAULT_PORT.toString())
        displayLocalIpAddress(ipAddressText)

        // Auto-start the server
        startServer(WiFiMouseService.DEFAULT_PORT, statusText)

        startButton.setOnClickListener {
            val port = portEditText.text.toString().toIntOrNull() ?: WiFiMouseService.DEFAULT_PORT
            startServer(port, statusText)
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, WiFiMouseService::class.java)
            stopService(serviceIntent)
            statusText.text = "Server stopped"
        }
    }

    private fun startServer(port: Int, statusText: TextView) {
        val serviceIntent = Intent(this, WiFiMouseService::class.java)
        serviceIntent.putExtra(WiFiMouseService.KEY_PORT, port)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        statusText.text = "Server started on port $port"
    }

    private fun displayLocalIpAddress(textView: TextView) {
        val ipAddress = getLocalIpAddress()
        textView.text = ipAddress ?: "Unable to get IP"
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .find { it is Inet4Address && !it.isLoopbackAddress && it.hostAddress?.isNotEmpty() == true }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
