package com.example.opendtuapp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LiveDashboardActivity : AppCompatActivity() {
    private lateinit var dataTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 30) }
        dataTextView = TextView(this).apply { text = "Loading live data..." }
        layout.addView(dataTextView)
        setContentView(layout)

        fetchLiveData()
    }

    private fun fetchLiveData() {
        ApiClient(this).get("/api/livedata/status") { response ->
            runOnUiThread {
                // For simplicity, we display the raw JSON. 
                // In a production app, use Gson to parse specific fields.
                dataTextView.text = response ?: "Error fetching data"
            }
        }
    }
}
