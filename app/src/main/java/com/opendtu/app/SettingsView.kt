package com.opendtu.app

import android.content.Context
import android.widget.*

class SettingsView(private val context: Context) {
    
    // onUpdateCheck is a callback function passed from MainActivity
    fun load(container: FrameLayout, onUpdateCheck: () -> Unit) {
        container.removeAllViews()
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val btnUpdate = Button(context).apply {
            text = "Check for App Update (GitHub)"
            setOnClickListener { onUpdateCheck() }
        }

        val btnReboot = Button(context).apply {
            text = "Reboot OpenDTU"
            setOnClickListener {
                ApiClient(context).post("/api/maintenance/reboot", "{}") { success ->
                    (context as MainActivity).runOnUiThread {
                        Toast.makeText(context, "Reboot triggered: $success", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val infoText = TextView(context).apply {
            text = "\nDTU Endpoints:"
            textSize = 14f
        }

        // List of common API endpoints for quick debugging
        val endpoints = listOf("/api/network/status", "/api/mqtt/status", "/api/system/status")
        
        layout.addView(btnUpdate)
        layout.addView(btnReboot)
        layout.addView(infoText)

        endpoints.forEach { path ->
            val btn = Button(context).apply {
                text = "View $path"
                setOnClickListener {
                    ApiClient(context).get(path) { resp ->
                        (context as MainActivity).runOnUiThread {
                            Toast.makeText(context, resp ?: "Request failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            layout.addView(btn)
        }

        container.addView(layout)
    }
}
