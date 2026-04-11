package com.opendtu.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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
            setPadding(40, 40, 40, 40)
        }

        // Title Header
        val header = TextView(context).apply {
            text = "Echtzeit Daten"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
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
                    Log.e(TAG, "Fehler beim Abrufen der Live-Daten: $error")
                    showError(error)
                } else if (response != null) {
                    Log.d(TAG, "Daten erfolgreich empfangen")
                    parseAndDisplay(response)
                }
            }
        }
    }

    private fun parseAndDisplay(jsonString: String) {
        try {
            mainLayout.removeAllViews()
            
            // Re-add Header
            mainLayout.addView(TextView(context).apply {
                text = "Live Dashboard"
                textSize = 22f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, 30)
            })

            val json = JSONObject(jsonString)
            val total = json.optJSONObject("total") // OpenDTU fasst Gesamtwerte unter "total" zusammen

            if (total != null) {
                addCard("Gesamtleistung", "${total.optString("Power", "0")} W", Color.parseColor("#4CAF50"))
                addInfoRow("Ertrag Heute", "${total.optString("YieldDay", "0")} Wh")
                addInfoRow("Ertrag Gesamt", "${total.optString("YieldTotal", "0")} kWh")
            }

            // Inverter Details
            val inverters = json.optJSONArray("inverters")
            if (inverters != null && inverters.length() > 0) {
                for (i in 0 until inverters.length()) {
                    val inv = inverters.getJSONObject(i)
                    addSeparator()
                    addInfoRow("Wechselrichter", inv.optString("name", "Unbekannt"), true)
                    addInfoRow("Status", if(inv.optBoolean("reachable")) "Online" else "Offline")
                    addInfoRow("Aktuell", "${inv.optString("Power", "0")} W")
                }
            }

            // Refresh Button am Ende
            val refreshBtn = Button(context).apply {
                text = "Aktualisieren"
                setOnClickListener { refreshData() }
            }
            mainLayout.addView(refreshBtn)

        } catch (e: Exception) {
            Log.e(TAG, "Parsing Fehler: ${e.message}")
            showError("Fehler beim Lesen der Daten: ${e.localizedMessage}")
        }
    }

    private fun addCard(label: String, value: String, bgColor: Int) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(bgColor)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 30)
            layoutParams = params
        }
        val lbl = TextView(context).apply { text = label; textColor = Color.WHITE; textSize = 14f }
        val valTxt = TextView(context).apply { 
            text = value
            textColor = Color.WHITE
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
        }
        card.addView(lbl)
        card.addView(valTxt)
        mainLayout.addView(card)
    }

    private fun addInfoRow(label: String, value: String, isBold: Boolean = false) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
            weightSum = 1f
        }
        val lbl = TextView(context).apply { 
            text = label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
        }
        val valTxt = TextView(context).apply { 
            text = value
            gravity = Gravity.END
            if (isBold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
        }
        row.addView(lbl)
        row.addView(valTxt)
        mainLayout.addView(row)
    }

    private fun addSeparator() {
        val view = FrameLayout(context).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            params.setMargins(0, 20, 0, 20)
            layoutParams = params
            setBackgroundColor(Color.LTGRAY)
        }
        mainLayout.addView(view)
    }

    private fun showStatus(msg: String) {
        mainLayout.removeAllViews()
        mainLayout.addView(TextView(context).apply { text = msg; gravity = Gravity.CENTER })
    }

    private fun showError(error: String) {
        mainLayout.removeAllViews()
        val errorTv = TextView(context).apply {
            text = "⚠️\n$error"
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(0, 50, 0, 20)
        }
        val retryBtn = Button(context).apply {
            text = "Erneut versuchen"
            setOnClickListener { refreshData() }
        }
        mainLayout.addView(errorTv)
        mainLayout.addView(retryBtn)
    }
}
