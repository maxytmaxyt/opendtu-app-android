package com.opendtu.app

import android.content.Context
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class SettingsView(private val context: Context) {
    fun load(container: FrameLayout, onUpdateCheck: () -> Unit) {
        container.removeAllViews()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val btnUpdate = Button(context).apply {
            text = "Check for App Update"
            setOnClickListener { onUpdateCheck() }
        }

        val btnReboot = Button(context).apply {
            text = "Reboot DTU"
            setOnClickListener {
                ApiClient(context).post("/api/maintenance/reboot", "{}") { }
            }
        }

        layout.addView(btnUpdate)
        layout.addView(btnReboot)
        container.addView(layout)
    }
}
