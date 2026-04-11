package com.example.opendtuapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class InverterManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply { text = "Inverter Control"; textSize = 20f }
        layout.addView(title)

        // Example: Set Power Limit
        val limitInput = EditText(this).apply { hint = "Limit in Watts" }
        val setLimitBtn = Button(this).apply {
            text = "Set Limit"
            setOnClickListener {
                val json = "{\"limit_type\": 0, \"limit_value\": ${limitInput.text}}"
                ApiClient(this@InverterManagementActivity).post("/api/limit/config", json) { success ->
                    runOnUiThread { Toast.makeText(context, "Success: $success", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        layout.addView(limitInput)
        layout.addView(setLimitBtn)

        // Load Inverter List
        val listBtn = Button(this).apply {
            text = "Fetch Inverter List"
            setOnClickListener {
                ApiClient(this@InverterManagementActivity).get("/api/inverter/list") { resp ->
                    runOnUiThread { Toast.makeText(context, resp ?: "No inverters found", Toast.LENGTH_LONG).show() }
                }
            }
        }
        layout.addView(listBtn)

        setContentView(layout)
    }
}
