package com.vesputi.mobilitybox_ticketing_android.models

import android.util.Log
import okhttp3.*
import okio.IOException
import java.net.URL

object MobilityboxTicketRenderingEngine {
    var engineCode: String? = null
    var engineString: String? = null

    init {
        loadEngine()
        updateEngine()
    }

    fun loadEngine() {
        val preferences = Mobilitybox.preferences
        val mobilityboxRenderingEngineString = preferences.getString("mobilityboxRenderingEngineString", null)
        this.engineString = mobilityboxRenderingEngineString
    }

    fun saveEngine(engineString: String) {
        this.engineString = engineString
        val preference = Mobilitybox.preferences

        with (preference.edit()) {
            putString("mobilityboxRenderingEngineString", engineString)
            apply()
        }
    }

    fun updateEngine() {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/rendering_engine/1?inline=inline")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_FETCH_ENGINE", "Error fetching Rendering ENGINE")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                saveEngine(body.toString())
            }
        })
    }
}

data class MobilityboxRenderingEngineData(val engineCode: String, val engineString: String)