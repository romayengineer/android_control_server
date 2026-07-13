package com.romayengineer.controlserver

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private const val TAG = "WiFiMouseServer"
    private const val MAX_LOGS = 500
    private val logs = mutableListOf<String>()
    private var logTextView: TextView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setLogTextView(textView: TextView?) {
        logTextView = textView
        updateUI()
    }

    fun d(message: String, tag: String = TAG) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        Log.d(tag, message)
        addLog(logEntry)
    }

    fun i(message: String, tag: String = TAG) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        Log.i(tag, message)
        addLog(logEntry)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] ERROR: $message"
        Log.e(tag, message, throwable)
        addLog(logEntry)
    }

    fun w(message: String, tag: String = TAG) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] WARNING: $message"
        Log.w(tag, message)
        addLog(logEntry)
    }

    private fun addLog(logEntry: String) {
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
        }
        updateUI()
    }

    private fun updateUI() {
        mainHandler.post {
            logTextView?.let {
                synchronized(logs) {
                    it.text = logs.joinToString("\n")
                }
                it.post {
                    val scrollAmount = it.layout?.getLineTop(it.lineCount) ?: 0
                    if (scrollAmount > it.height) {
                        it.scrollTo(0, scrollAmount - it.height)
                    }
                }
            }
        }
    }

    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        updateUI()
    }

    fun getLogs(): String {
        synchronized(logs) {
            return logs.joinToString("\n")
        }
    }
}
