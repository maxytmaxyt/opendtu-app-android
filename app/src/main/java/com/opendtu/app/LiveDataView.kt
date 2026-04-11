package com.opendtu.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class LiveDataView(private val context: Context) {
    private val TAG = "OpenDTU_LiveView"
    private lateinit var mainLayout: LinearLayout

    fun load(container: FrameLayout) {
        container.removeAllViews()

        val scroll = ScrollView(context)
        mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }

        val header = TextView(context).apply {
            text = "Live Dashboard"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(header)

        scroll.addView(mainLayout)
        container.addView(scroll)

        refreshData()
    }

    private fun refreshData() {
        showStatus("Lade Daten...")

        ApiClient(context).get("/api/livedata/status") { response, error ->
            (context as MainActivity).runOnUiThread {
                if (error != null) {
                    Log.e(TAG, "Fetch error: $error")
                    showError(error)
                } else if (response != null) {
                    parseAndDisplay(response)
                }
            }
        }
    }

    private fun parseAndDisplay(jsonString: String) {
        try {
            mainLayout.removeAllViews()

            val header = TextView(context).apply {
                text = "Live Dashboard"
                textSize = 28f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, 0, 0, 40)
            }
            mainLayout.addView(header)

            val json = JSONObject(jsonString)
            val total = json.optJSONObject("total")

            if (total != null) {
                val power = extractValue(total, "Power")
                val yieldDay = extractValue(total, "YieldDay")
                val yieldTotal = extractValue(total, "YieldTotal")

                addMainCard("Gesamtleistung", "$power W", Color.parseColor("#4CAF50"))
                
                addInfoCard("Ertragsübersicht", listOf(
                    Pair("Heute", "$yieldDay Wh"),
                    Pair("Gesamt", "$yieldTotal kWh")
                ))
            }

            val inverters = json.optJSONArray("inverters")
            if (inverters != null && inverters.length() > 0) {
                for (i in 0 until inverters.length()) {
                    val inv = inverters.getJSONObject(i)
                    val name = inv.optString("name", "Unbekannt")
                    val reachable = inv.optBoolean("reachable", false)
                    val statusText = if (reachable) "Online" else "Offline"
                    
                    // OpenDTU inverter power is often nested inside AC -> 0 -> Power
                    var invPower = extractValue(inv, "Power")
                    if (invPower == "0" && inv.has("AC")) {
                        val ac = inv.optJSONObject("AC")
                        val phase = ac?.optJSONObject("0") ?: ac?.optJSONObject("1")
                        if (phase != null) {
                            invPower = extractValue(phase, "Power")
                        }
                    }

                    addInfoCard("Wechselrichter: $name", listOf(
                        Pair("Status", statusText),
                        Pair("Aktuell", "$invPower W")
                    ))
                }
            }

            val refreshBtn = Button(context).apply {
                text = "Aktualisieren"
                setBackgroundColor(Color.parseColor("#2196F3"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    150
                ).apply { setMargins(0, 40, 0, 40) }
                
                setOnClickListener { refreshData() }
            }
            mainLayout.addView(refreshBtn)

        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            showError("Fehler beim Verarbeiten der Daten.")
        }
    }

    // Extracts the actual value ("v") from the OpenDTU nested value object
    private fun extractValue(parent: JSONObject, key: String): String {
        val obj = parent.optJSONObject(key)
        return obj?.optString("v", "0") ?: "0"
    }

    private fun addMainCard(label: String, value: String, bgColor: Int) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 30f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }
        
        val lbl = TextView(context).apply { 
            text = label
            setTextColor(Color.argb(220, 255, 255, 255))
            textSize = 14f 
        }
        val valTxt = TextView(context).apply { 
            text = value
            setTextColor(Color.WHITE)
            textSize = 36f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 0)
        }
        
        card.addView(lbl)
        card.addView(valTxt)
        mainLayout.addView(card)
    }

    private fun addInfoCard(title: String, rows: List<Pair<String, String>>) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 30f
                setStroke(2, Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 0, 0, 20)
        }
        card.addView(titleView)

        rows.forEach { (label, value) ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 15, 0, 15)
                weightSum = 1f
            }
            val lbl = TextView(context).apply { 
                text = label
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            }
            val valTxt = TextView(context).apply { 
                text = value
                setTextColor(Color.parseColor("#333333"))
                gravity = Gravity.END
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            }
            row.addView(lbl)
            row.addView(valTxt)
            card.addView(row)
        }
        
        mainLayout.addView(card)
    }

    private fun showStatus(msg: String) {
        mainLayout.removeAllViews()
        mainLayout.addView(TextView(context).apply { 
            text = msg
            gravity = Gravity.CENTER
            setPadding(0, 100, 0, 0)
            setTextColor(Color.parseColor("#666666"))
        })
    }

    private fun showError(error: String) {
        mainLayout.removeAllViews()
        val errorTv = TextView(context).apply {
            text = "⚠️\n$error"
            setTextColor(Color.parseColor("#D32F2F"))
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(0, 100, 0, 40)
        }
        val retryBtn = Button(context).apply {
            text = "Erneut versuchen"
            setBackgroundColor(Color.parseColor("#D32F2F"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                150
            ).apply { setMargins(0, 40, 0, 0) }
            setOnClickListener { refreshData() }
        }
        mainLayout.addView(errorTv)
        mainLayout.addView(retryBtn)
    }
}
