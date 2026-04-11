package com.example.opendtuapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40) 
        }

        val scroll = ScrollView(this)
        scroll.addView(container)

        val sections = listOf("/api/network/config", "/api/mqtt/config", "/api/ntp/config")
        
        sections.forEach { endpoint ->
            val btn = Button(this).apply {
                text = "Load Config: $endpoint"
                setOnClickListener {
                    ApiClient(this@SettingsActivity).get(endpoint) { resp ->
                        runOnUiThread { Toast.makeText(context, resp ?: "Fail", Toast.LENGTH_LONG).show() }
                    }
                }
            }
            container.addView(btn)
        }

        setContentView(scroll)
    }
}
