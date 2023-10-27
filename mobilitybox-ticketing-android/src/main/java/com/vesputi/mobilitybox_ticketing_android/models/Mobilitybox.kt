package com.vesputi.mobilitybox_ticketing_android.models

import android.content.SharedPreferences

object Mobilitybox {
    lateinit var preferences: SharedPreferences
    lateinit var api: MobilityboxApi
    lateinit var renderingEngine: MobilityboxTicketRenderingEngine
    lateinit var identificationViewEngine: MobilityboxIdentificationViewEngine

    init {
        println("Mobilitybox class invoked.")
    }

    fun setup(preferences: SharedPreferences, apiConfig: MobilityboxApi.MobilityboxApiConfig? = null) {
        this.preferences = preferences
        this.api = MobilityboxApi
        this.api.setup(apiConfig)

        this.renderingEngine = MobilityboxTicketRenderingEngine
        this.identificationViewEngine = MobilityboxIdentificationViewEngine
    }
}

object MobilityboxApi {
    init {
        println("MobilityboxApi class invoked.")
    }

    var apiUrl = "https://api.themobilitybox.com/v4"

    fun setup(apiConfig: MobilityboxApiConfig? = null) {
        if (apiConfig?.apiURL != null) {
            this.apiUrl = apiConfig.apiURL
        }
    }

    data class MobilityboxApiConfig(val apiURL: String) {}
}



enum class MobilityboxError {
    UNKOWN, RETRY_LATER, NOT_REACTIVATABLE
}