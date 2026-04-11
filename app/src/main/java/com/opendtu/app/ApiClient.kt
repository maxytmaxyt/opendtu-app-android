package com.opendtu.app

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient(context: Context) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val baseUrl: String get() = "http://${prefs.getString("dtu_ip", "")}"

    fun get(endpoint: String, callback: (String?) -> Unit) {
        val request = Request.Builder().url("$baseUrl$endpoint").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string())
            }
        })
    }

    fun post(endpoint: String, jsonBody: String, callback: (Boolean) -> Unit) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("$baseUrl$endpoint").post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false)
            override fun onResponse(call: Call, response: Response) = callback(response.isSuccessful)
        })
    }
}
