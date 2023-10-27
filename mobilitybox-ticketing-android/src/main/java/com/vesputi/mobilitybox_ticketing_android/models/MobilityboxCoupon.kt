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
    var tariff_settings: JsonElement?
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
        Gson().fromJson(parcel.readString(), JsonElement::class.java)
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

    fun reactivate(reactivation_key: String, completion: (MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val body = mapOf("reactivation_key" to reactivation_key)
        val gson = GsonBuilder().create()
        val bodyJSON = gson.toJson(body)
        activateCall(bodyJSON, completion, failure)
    }

    private fun activateCall(bodyJSON: String, completion: (MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
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
                        failure(MobilityboxError.UNKOWN)
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
        return "${this.area.properties.city_name} - ${this.product.getTitle()}"
    }

    fun getDescription(): (String) {
        return "${product.getDescription()} In der folgenden Tarifzone: ${area.properties.local_zone_name}"
    }

    fun tariffSettingsToString(): (String) {
        return Gson().toJson(tariff_settings)
    }

    private data class ActivateCouponResponse(val ticket_id: String)

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