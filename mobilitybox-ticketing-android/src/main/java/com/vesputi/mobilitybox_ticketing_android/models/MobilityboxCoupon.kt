package com.mobilitybox.android.models

import android.os.Parcelable
import android.util.Log
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxApi
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxError
import com.google.gson.GsonBuilder
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
    var product: MobilityboxProduct,
    var area: MobilityboxArea,
    var activated: Boolean,
    var environment: String,
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
                    completion()
                }
            }
        })
    }

    fun activate(identificationMedium: MobilityboxIdentificationMedium, completion: (MobilityboxTicketCode) -> (Unit)) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/coupons/${this.id}/activate.json")
        val body = identificationMedium.identificationMediumJson
        Log.d("DEBUG_ACTIVATE_URL", url.toString())
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
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