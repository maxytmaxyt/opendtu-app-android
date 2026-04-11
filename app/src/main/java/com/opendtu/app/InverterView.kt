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
            text = "Wechselrichter Steuerung"
            textSize = 20f
        }
        
        val limitInput = EditText(context).apply {
            hint = "Limit in Watt"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val btnSetLimit = Button(context).apply {
            text = "Limit setzen"
            setOnClickListener {
                val watts = limitInput.text.toString()
                if (watts.isNotEmpty()) {
                    val json = "{\"limit_type\": 0, \"limit_value\": $watts}"
                    ApiClient(context).post("/api/limit/config", json) { success, error ->
                        (context as MainActivity).runOnUiThread {
                            val msg = if (success) "Limit erfolgreich gesetzt" else "Fehler: $error"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val listBtn = Button(context).apply {
            text = "Liste aktualisieren"
            setOnClickListener {
                ApiClient(context).get("/api/inverter/list") { resp, error ->
                    (context as MainActivity).runOnUiThread {
                        if (error != null) {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Inverter Daten geladen", Toast.LENGTH_SHORT).show()
                        }
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
