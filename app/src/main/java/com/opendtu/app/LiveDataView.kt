package com.opendtu.app

import android.content.Context
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LiveDataView(private val context: Context) {
    fun load(container: FrameLayout) {
        container.removeAllViews()
        
        val scroll = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        val statusText = TextView(context).apply {
            text = "Fetching live inverter data..."
            textSize = 16f
        }
        
        layout.addView(statusText)
        scroll.addView(layout)
        container.addView(scroll)

        // Request real-time status from DTU
        ApiClient(context).get("/api/livedata/status") { response ->
            (context as MainActivity).runOnUiThread {
                statusText.text = response ?: "Error: Could not reach DTU. Check IP and connection."
            }
        }
    }
}
