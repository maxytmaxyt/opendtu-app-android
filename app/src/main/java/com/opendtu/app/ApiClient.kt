package com.opendtu.app

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient(context: Context) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    
    // Construct the base URL from the stored IP address
    private val baseUrl: String 
        get() = "http://${prefs.getString("dtu_ip", "")}"

    // Performs an asynchronous GET request
    fun get(endpoint: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Return null if the network call fails
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                // Return the response body as a string
                callback(response.body?.string())
            }
        })
    }

    // Performs an asynchronous POST request with a JSON body
    fun post(endpoint: String, jsonBody: String, callback: (Boolean) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }
}
