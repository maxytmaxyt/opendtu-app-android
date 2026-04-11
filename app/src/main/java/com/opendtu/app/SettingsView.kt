package com.opendtu.app

import android.content.Context
import android.widget.*

class SettingsView(private val context: Context) {
    
    fun load(container: FrameLayout, onUpdateCheck: () -> Unit) {
        container.removeAllViews()
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val btnUpdate = Button(context).apply {
            text = "Auf App-Update prüfen"
            setOnClickListener { onUpdateCheck() }
        }

        val btnReboot = Button(context).apply {
            text = "OpenDTU neu starten"
            setOnClickListener {
                ApiClient(context).post("/api/maintenance/reboot", "{}") { success, error ->
                    (context as MainActivity).runOnUiThread {
                        val msg = if (success) "DTU wird neu gestartet..." else "Fehler: $error"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val endpoints = listOf("/api/network/status", "/api/system/status")
        
        layout.addView(btnUpdate)
        layout.addView(btnReboot)

        endpoints.forEach { path ->
            val btn = Button(context).apply {
                text = "Debug $path"
                setOnClickListener {
                    ApiClient(context).get(path) { resp, error ->
                        (context as MainActivity).runOnUiThread {
                            if (error != null) {
                                Toast.makeText(context, "Fehler: $error", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Daten empfangen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            layout.addView(btn)
        }

        container.addView(layout)
    }
}
