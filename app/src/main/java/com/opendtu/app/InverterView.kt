package com.opendtu.app

import android.content.Context
import android.widget.*

class InverterView(private val context: Context) {
    fun load(container: FrameLayout) {
        container.removeAllViews()
        
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(context).apply {
            text = "Inverter Management"
            textSize = 20f
        }
        
        val limitInput = EditText(context).apply {
            hint = "New Limit (Watts)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val btnSetLimit = Button(context).apply {
            text = "Set Power Limit"
            setOnClickListener {
                val watts = limitInput.text.toString()
                if (watts.isNotEmpty()) {
                    // JSON structure for OpenDTU power limit API
                    val json = "{\"limit_type\": 0, \"limit_value\": $watts}"
                    ApiClient(context).post("/api/limit/config", json) { success ->
                        (context as MainActivity).runOnUiThread {
                            Toast.makeText(context, "Limit set: $success", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val listBtn = Button(context).apply {
            text = "Show Inverter List (JSON)"
            setOnClickListener {
                ApiClient(context).get("/api/inverter/list") { resp ->
                    (context as MainActivity).runOnUiThread {
                        Toast.makeText(context, resp ?: "No data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        layout.addView(title)
        layout.addView(limitInput)
        layout.addView(btnSetLimit)
        layout.addView(listBtn)
        container.addView(layout)
    }
}
