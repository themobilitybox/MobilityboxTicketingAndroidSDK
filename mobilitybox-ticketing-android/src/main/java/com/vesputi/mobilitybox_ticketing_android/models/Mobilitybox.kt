package com.vesputi.mobilitybox_ticketing_android.models

import android.content.SharedPreferences
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

    var apiUrl = "https://api.themobilitybox.com/v7"

    fun setup(apiConfig: MobilityboxApiConfig? = null) {
        if (apiConfig?.apiURL != null) {
            this.apiUrl = apiConfig.apiURL
        }
    }

    data class MobilityboxApiConfig(val apiURL: String) {}
}

@Parcelize
enum class MobilityboxError : Parcelable {
    UNKOWN, RETRY_LATER, NOT_REACTIVATABLE, GOOGLE_PASS_NOT_POSSIBLE, GOOGLE_PASS_NOT_AVAILABLE, IDENTIFICATION_MEDIUM_NOT_VALID, TARIFF_SETTINGS_NOT_VALID, BEFORE_EARLIEST_ACTIVATION_START_DATETIME, COUPON_ACTIVATION_EXPIRED
}