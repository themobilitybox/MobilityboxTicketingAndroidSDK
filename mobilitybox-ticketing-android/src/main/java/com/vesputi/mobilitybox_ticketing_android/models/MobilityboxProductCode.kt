package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okio.IOException
import java.net.URL
import java.util.*

@Parcelize
class MobilityboxProductCode(
    val productId: String
) : Parcelable {

    fun fetchProduct(completion: (product: MobilityboxProduct) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/products/${this.productId}.json")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_FETCH_PRODUCT", "Error fetching Product")
                if (failure != null) {
                    failure(MobilityboxError.UNKOWN)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                if (response.code != 200) {
                    Log.e("ERROR_FETCH_PRODUCT", "Error fetching Product, Error: ${body}")
                    if (failure != null) {
                        failure(MobilityboxError.UNKOWN)
                    }
                } else {
                    val gson = GsonBuilder().create()
                    Log.d("DEBUG_PRODUCT_BODY", body.toString())
                    val product = gson.fromJson(body, MobilityboxProduct::class.java)
                    if (product != null) {
                        completion(product)
                    }
                }

            }
        })
    }
}