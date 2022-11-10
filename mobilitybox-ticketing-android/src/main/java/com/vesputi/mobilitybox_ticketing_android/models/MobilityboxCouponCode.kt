package com.mobilitybox.android.models

import android.os.Parcelable
import android.util.Log
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxApi
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxError
import com.google.gson.GsonBuilder
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okio.IOException
import java.net.URL
import java.util.*

@Parcelize
class MobilityboxCouponCode(
    val couponId: String
) : Parcelable {

    fun fetchCoupon(completion: (coupon: MobilityboxCoupon) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/coupons/${this.couponId}.json")
        Log.d("DEBUG_COUPON_URL", url.toString())
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
                if (response.code != 200) {
                    Log.e("ERROR_FETCH_COUPON", "Error fetching Coupon, Error: ${body}")
                    if (failure != null) {
                        failure(MobilityboxError.UNKOWN)
                    }
                } else {
                    val gson = GsonBuilder().create()
                    Log.d("DEBUG_COUPON_BODY", body.toString())
                    val coupon = gson.fromJson(body, MobilityboxCoupon::class.java)
                    if (coupon != null) {
                        coupon.createdAt = Date()
                        completion(coupon)
                    }
                }

            }
        })
    }
}