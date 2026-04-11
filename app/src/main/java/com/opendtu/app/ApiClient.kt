package com.opendtu.app

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient(private val context: Context) {
    private val TAG = "OpenDTU_Api"
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private val baseUrl: String get() = "http://${prefs.getString("dtu_ip", "")}"
    
    // Credentials dynamically loaded from storage
    private fun getAuthHeader(): String {
        val user = prefs.getString("dtu_user", "admin") ?: "admin"
        val pass = prefs.getString("dtu_pass", "openDTU42") ?: "openDTU42"
        return Credentials.basic(user, pass)
    }

    fun get(endpoint: String, callback: (String?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .header("Authorization", getAuthHeader())
            .build()

        Log.d(TAG, "GET Request to: ${baseUrl}${endpoint}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Failure: ${e.message}")
                callback(null, "Netzwerkfehler: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                when (response.code) {
                    200 -> callback(body, null)
                    401 -> callback(null, "Fehler 401: Benutzername oder Passwort falsch.")
                    404 -> callback(null, "Fehler 404: Endpunkt nicht gefunden.")
                    else -> callback(null, "Fehler ${response.code}: ${response.message}")
                }
            }
        })
    }

    fun post(endpoint: String, jsonBody: String, callback: (Boolean, String?) -> Unit) {
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .header("Authorization", getAuthHeader())
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "POST Failure: ${e.message}")
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(true, null)
                } else {
                    Log.e(TAG, "POST Error: ${response.code}")
                    callback(false, "Status ${response.code}")
                }
            }
        })
    }
}
