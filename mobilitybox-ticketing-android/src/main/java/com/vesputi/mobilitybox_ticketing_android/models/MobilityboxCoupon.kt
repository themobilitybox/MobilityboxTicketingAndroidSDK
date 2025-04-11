package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.internal.bind.util.ISO8601Utils
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.URL
import java.util.Date

class MobilityboxCoupon(
    val id: String,
    var original_coupon_id: String?,
    var restored_coupon_id: String?,
    var product: MobilityboxProduct,
    var area: MobilityboxArea,
    var activated: Boolean,
    var environment: String,
    var subscription: MobilityboxSubscription?,
    var createdAt: Date? = Date(),
    var tariff_settings_valid: Boolean?,
    var tariff_settings: JsonElement?,
    var earliest_activation_start_datetime: String?,
    var latest_activation_start_datetime: String?
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(MobilityboxProduct::class.java.classLoader)!!,
        parcel.readParcelable(MobilityboxArea::class.java.classLoader)!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString(),
        parcel.readParcelable(MobilityboxSubscription::class.java.classLoader),
        Date(parcel.readLong()),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        Gson().fromJson(parcel.readString(), JsonElement::class.java),
        parcel.readString(),
        parcel.readString()
    ) {
        if (this.createdAt?.time ?: -1L == -1L) {
            this.createdAt = null
        }
    }

    fun update(completion: () -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/coupons/${this.id}.json")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_FETCH_COUPON", "Error fetching Coupon")
                if (failure != null) {
                    failure(MobilityboxError.UNKOWN)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                val gson = GsonBuilder().create()
                val updatedCoupon = gson.fromJson(body, MobilityboxCoupon::class.java)

                if (updatedCoupon != null) {
                    product = updatedCoupon.product
                    area = updatedCoupon.area
                    activated = updatedCoupon.activated
                    original_coupon_id = updatedCoupon.original_coupon_id
                    restored_coupon_id = updatedCoupon.restored_coupon_id
                    subscription = updatedCoupon.subscription
                    tariff_settings = updatedCoupon.tariff_settings
                    tariff_settings_valid = updatedCoupon.tariff_settings_valid
                    earliest_activation_start_datetime = updatedCoupon.earliest_activation_start_datetime
                    latest_activation_start_datetime = updatedCoupon.latest_activation_start_datetime

                    completion()
                }
            }
        })
    }
    @JvmOverloads
    fun activate(identificationMedium: MobilityboxIdentificationMedium, completion: (MobilityboxTicketCode) -> (Unit), activationStartDateTime: Date? = null, failure: ((error: MobilityboxError) -> Unit)? = null) {
        val bodyJSON = if (activationStartDateTime != null) {
            val gson = GsonBuilder().create()
            var identificationMedium = gson.fromJson(identificationMedium.identificationMediumJson, JsonObject::class.java)
            var body = JsonObject()
            if (original_coupon_id == null) {
                var formattedActivationStartDateTime = ISO8601Utils.format(activationStartDateTime).toString()
                body.addProperty("activation_start_datetime", formattedActivationStartDateTime)
            }
            body.add("identification_medium", identificationMedium.get("identification_medium"))
            gson.toJson(body)
        } else {
            identificationMedium.identificationMediumJson
        }
        activateCall(bodyJSON, completion, failure)
    }

    @JvmOverloads
    fun activate(identificationMedium: MobilityboxIdentificationMedium, tariffSettings: MobilityboxTariffSettings, completion: (MobilityboxTicketCode) -> (Unit), activationStartDateTime: Date? = null, failure: ((error: MobilityboxError) -> Unit)? = null) {
        val gson = GsonBuilder().create()
        var identificationMedium = gson.fromJson(identificationMedium.identificationMediumJson, JsonObject::class.java)
        var tariffSettings = gson.fromJson(tariffSettings.tariffSettingsJson, JsonObject::class.java)
        var body = JsonObject()
        if (original_coupon_id == null && activationStartDateTime != null) {
            var formattedActivationStartDateTime = ISO8601Utils.format(activationStartDateTime).toString()
            body.addProperty("activation_start_datetime", formattedActivationStartDateTime)
        }
        body.add("identification_medium", identificationMedium.get("identification_medium"))
        body.add("tariff_settings", tariffSettings.get("tariff_settings"))
        val bodyJSON = gson.toJson(body)
        activateCall(bodyJSON, completion, failure)
    }

    fun reactivate(reactivation_key: String, completion: (MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val body = mapOf("reactivation_key" to reactivation_key)
        val gson = GsonBuilder().create()
        val bodyJSON = gson.toJson(body)
        activateCall(bodyJSON, completion, failure)
    }

    fun reactivate(reactivation_key: String, identificationMedium: MobilityboxIdentificationMedium? = null, tariffSettings: MobilityboxTariffSettings? = null, completion: (MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val gson = GsonBuilder().create()
        var body = JsonObject()
        body.addProperty("reactivation_key", reactivation_key)

        if (identificationMedium != null) {
            var identificationMedium = gson.fromJson(identificationMedium.identificationMediumJson, JsonObject::class.java)
            body.add("identification_medium", identificationMedium.get("identification_medium"))
        }
        if (tariffSettings != null) {
            var tariffSettings = gson.fromJson(tariffSettings.tariffSettingsJson, JsonObject::class.java)
            body.add("tariff_settings", tariffSettings.get("tariff_settings"))
        }

        val bodyJSON = gson.toJson(body)
        activateCall(bodyJSON, completion, failure)
    }

    fun activateCall(bodyJSON: String, completion: (MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/coupons/${this.id}/activate.json")
        Log.d("DEBUG_ACTIVATE_URL", url.toString())
        val request = Request.Builder()
            .url(url)
            .post(bodyJSON.toRequestBody("application/json".toMediaType()))
            .build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_ACTIVATE_COUPON", "Error activating Coupon")
                if (failure != null) {
                    failure(MobilityboxError.UNKOWN)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                if (response.code != 201) {
                    if (failure != null) {
                        if (response.code == 422 || response.code == 400) {
                            val gson = GsonBuilder().create()
                            val data = gson.fromJson(body, ActivateCouponErrorResponse::class.java)
                            if (data.message.startsWith("identification_medium:")) {
                                failure(MobilityboxError.IDENTIFICATION_MEDIUM_NOT_VALID)
                            } else if (data.message.startsWith("tariff_settings:")) {
                                failure(MobilityboxError.TARIFF_SETTINGS_NOT_VALID)
                            } else if (data.message.startsWith("Ticket cannot be activated yet")) {
                                failure(MobilityboxError.BEFORE_EARLIEST_ACTIVATION_START_DATETIME)
                            } else if (data.message.startsWith("Ticket cannot be activated anymore")) {
                                failure(MobilityboxError.COUPON_ACTIVATION_EXPIRED)
                            } else {
                                failure(MobilityboxError.UNKOWN)
                            }
                        } else {
                            failure(MobilityboxError.UNKOWN)
                        }
                    }
                } else {
                    val gson = GsonBuilder().create()
                    Log.d("DEBUG_TICKET_CODE_BODY", body.toString())
                    val data = gson.fromJson(body, ActivateCouponResponse::class.java)
                    completion(MobilityboxTicketCode(data.ticket_id))
                }
            }
        })
    }

    fun getTitle(): (String) {
        return product.local_ticket_name
    }

    fun getDescription(): (String) {
        return "${if (product.getDescription().isBlank()) "" else product.getDescription() + " "}In der folgenden Tarifzone: ${area.properties.local_zone_name}"
    }

    fun isRestoredCoupon(): (Boolean) {
        return original_coupon_id != null
    }

    fun tariffSettingsToString(): (String) {
        return Gson().toJson(tariff_settings)
    }

    fun getReference(): (String) {
        return "C-${this.id.takeLast(6).uppercase()}"
    }

    private data class ActivateCouponResponse(val ticket_id: String)
    private data class ActivateCouponErrorResponse(val message: String)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(original_coupon_id)
        parcel.writeString(restored_coupon_id)
        parcel.writeParcelable(product, flags)
        parcel.writeParcelable(area, flags)
        parcel.writeByte(if (activated) 1 else 0)
        parcel.writeString(environment)
        parcel.writeParcelable(subscription, flags)
        parcel.writeLong(createdAt?.time ?: -1)
        parcel.writeValue(tariff_settings_valid)
        parcel.writeString(tariffSettingsToString())
        parcel.writeString(earliest_activation_start_datetime)
        parcel.writeString(latest_activation_start_datetime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MobilityboxCoupon> {
        override fun createFromParcel(parcel: Parcel): MobilityboxCoupon {
            return MobilityboxCoupon(parcel)
        }

        override fun newArray(size: Int): Array<MobilityboxCoupon?> {
            return arrayOfNulls(size)
        }
    }
}