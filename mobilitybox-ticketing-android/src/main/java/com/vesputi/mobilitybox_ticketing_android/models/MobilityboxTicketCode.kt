package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.parcelize.Parcelize
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.URL
import java.util.*

@Parcelize
class MobilityboxTicketCode(val ticketId: String, val couponId: String): Parcelable {

    fun fetchTicket(completion: (coupon: MobilityboxTicket) -> (Unit), failure: ((error: MobilityboxError) -> (Unit))? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/tickets/${this.ticketId}.json")
        Log.d("DEBUG_TICKET_URL", url.toString())
        val request = Request.Builder().url(url).patch("".toRequestBody()).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ERROR_FETCH_TICKET", "Error fetching Ticket")
                if (failure != null) {
                    failure(MobilityboxError.UNKOWN)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 202) {
                    if (failure != null) {
                        failure(MobilityboxError.RETRY_LATER)
                    }
                } else {
                    val body = response?.body?.string()
                    val gson = GsonBuilder().create()
                    Log.d("DEBUG_TICKET_BODY", body.toString())
                    var ticket = gson.fromJson(body, MobilityboxTicket::class.java)
                    ticket.createdAt = Date()
                    ticket.wasReactivated = false
                    completion(ticket)
                }
            }
        })
    }
}