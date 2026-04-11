package com.opendtu.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log // Wichtig: Import hinzugefügt
import android.view.Gravity
import android.widget.*

class SettingsView(private val context: Context) {
    
    // Updated signature to accept the firmware upgrade callback
    fun load(container: FrameLayout, onUpdateCheck: () -> Unit, onFirmwareUpgrade: () -> Unit) {
        container.removeAllViews()
        
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }

        val header = TextView(context).apply {
            text = "Einstellungen"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 0, 0, 50)
        }
        layout.addView(header)

        // --- SECTION: Maintenance ---
        addSectionHeader(layout, "Wartung & Updates")

        val btnUpdate = createStyledButton("Auf App-Update prüfen", "#2196F3") { 
            onUpdateCheck() 
        }
        
        val btnFirmware = createStyledButton("Firmware Upgrade (.bin)", "#FF9800") { 
            onFirmwareUpgrade() 
        }

        val btnReboot = createStyledButton("OpenDTU neu starten", "#F44336") {
            ApiClient(context).post("/api/maintenance/reboot", "{}") { success, error ->
                (context as MainActivity).runOnUiThread {
                    val msg = if (success) "Reboot gesendet!" else "Fehler: $error"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(btnUpdate)
        layout.addView(btnFirmware)
        layout.addView(btnReboot)

        // --- SECTION: Debug ---
        addSectionHeader(layout, "System Diagnose")

        val endpoints = listOf("/api/network/status", "/api/system/status")
        endpoints.forEach { path ->
            val btn = createStyledButton("Debug: $path", "#607D8B") {
                ApiClient(context).get(path) { resp, error ->
                    (context as MainActivity).runOnUiThread {
                        if (error != null) {
                            Toast.makeText(context, "Fehler: $error", Toast.LENGTH_SHORT).show()
                        } else {
                            // Now Log is correctly imported
                            Log.d("SettingsView", "API Response: $resp")
                            Toast.makeText(context, "Daten in Logcat ausgegeben", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            layout.addView(btn)
        }

        scroll.addView(layout)
        container.addView(scroll)
    }

    private fun addSectionHeader(layout: LinearLayout, title: String) {
        val tv = TextView(context).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.GRAY)
            setPadding(10, 40, 0, 20)
        }
        layout.addView(tv)
    }

    private fun createStyledButton(txt: String, colorHex: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = txt
            setTextColor(Color.WHITE)
            // Modern design with rounded corners instead of flat background
            val shape = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(Color.parseColor(colorHex))
            }
            background = shape
            
            // Support for all-caps off to look cleaner
            isAllCaps = false
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                150 // Slightly taller for better touch targets
            ).apply { setMargins(0, 0, 0, 30) }
            
            setOnClickListener { onClick() }
        }
    }
}
