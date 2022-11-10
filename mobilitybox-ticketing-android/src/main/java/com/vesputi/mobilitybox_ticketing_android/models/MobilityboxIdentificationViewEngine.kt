package com.mobilitybox.android.models

import android.util.Log
import com.vesputi.mobilitybox_ticketing_android.models.Mobilitybox
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxApi
import okhttp3.*
import okio.IOException
import java.net.URL

object MobilityboxIdentificationViewEngine {
    var engineCode: String? = null
    var engineString: String? = null

    init {
        loadEngine()
        updateEngine()
    }

    fun loadEngine() {
        val preferences = Mobilitybox.preferences
        val mobilityboxIdentificationViewEngineString = preferences.getString("mobilityboxIdentificationViewEngineString", null)
        this.engineString = mobilityboxIdentificationViewEngineString
    }

    fun saveEngine(engineString: String) {
        this.engineString = engineString
        val preference = Mobilitybox.preferences

        with (preference.edit()) {
            putString("mobilityboxIdentificationViewEngineString", engineString)
            apply()
        }
    }

    fun updateEngine() {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/identification_view/1?inline=inline")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_FETCH_ENGINE", "Error fetching Identification ENGINE")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                saveEngine(body.toString())
            }
        })
    }
}

data class MobilityboxIdentificationViewEngineData(val engineCode: String, val engineString: String)