package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.internal.bind.util.ISO8601Utils
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.URL
import java.util.Date

@Parcelize
class MobilityboxCoupon(
    val id: String,
    var original_coupon_id: String?,
    var restored_coupon_id: String?,
    var product: MobilityboxProduct,
    var area: MobilityboxArea,
    var activated: Boolean,
    var environment: String,
    var subscription: MobilityboxSubscription?,
    var createdAt: Date? = Date()
): Parcelable {

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
                    completion()
                }
            }
        })
    }
    @JvmOverloads
    fun activate(identificationMedium: MobilityboxIdentificationMedium, completion: (MobilityboxTicketCode) -> (Unit), activationStartDateTime: Date? = null) {
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
        activateCall(bodyJSON, completion)
    }

    fun reactivate(reactivation_key: String, completion: (MobilityboxTicketCode) -> (Unit)) {
        val body = mapOf("reactivation_key" to reactivation_key)
        val gson = GsonBuilder().create()
        val bodyJSON = gson.toJson(body)
        activateCall(bodyJSON, completion)
    }

    private fun activateCall(bodyJSON: String, completion: (MobilityboxTicketCode) -> (Unit)) {
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
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                val gson = GsonBuilder().create()
                Log.d("DEBUG_TICKET_CODE_BODY", body.toString())
                val data = gson.fromJson(body, ActivateCouponResponse::class.java)
                completion(MobilityboxTicketCode(data.ticket_id, id))
            }
        })
    }

    fun getTitle(): (String) {
        return "${this.area.properties.city_name} - ${this.product.getTitle()}"
    }

    fun getDescription(): (String) {
        return "${product.getDescription()} In der folgenden Tarifzone: ${area.properties.local_zone_name}"
    }

    private data class ActivateCouponResponse(val ticket_id: String)
}