package com.opendtu.app

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView

class InverterView(private val context: Context) {
    fun load(container: FrameLayout) {
        container.removeAllViews()
        val tv = TextView(context).apply { text = "Loading Inverters..." }
        container.addView(tv)

        ApiClient(context).get("/api/inverter/list") { response ->
            (context as MainActivity).runOnUiThread {
                tv.text = response ?: "No Inverters found"
            }
        }
    }
}
