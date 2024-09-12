package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.internal.bind.util.ISO8601Utils
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import okhttp3.*
import java.io.IOException
import java.net.URL
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
class MobilityboxTicket(
    val id: String,
    val coupon_id: String,
    val coupon_reactivation_key: String?,
    val product: MobilityboxOrderedProduct?,
    val ticket: MobilityboxTicketDetails?,
    val area: MobilityboxTicketArea?,
    val environment: String,
    val valid_from: String,
    val valid_until: String,
    val ticket_created_at: String,
    val sold_at: String,
    var createdAt: Date? = Date(),
    var wasReactivated: Boolean? = false
) : Parcelable {

    fun getTitle(): (String) {
        return product?.local_ticket_name ?: "${this.area?.properties?.city_name} - ${this.product?.getTitle()?.trim()}"
    }

    fun getDescription(): (String) {
        var formatter = SimpleDateFormat("dd. MMMM, HH:mm", Locale.GERMANY)

        return when (validity()) {
            MobilityboxTicketValidity.VALID -> {
                "gültig bis: ${formatter.format(ISO8601Utils.parse(valid_until, ParsePosition(0)))} Uhr"
            }
            MobilityboxTicketValidity.FUTURE -> {
                "gültig ab: ${formatter.format(ISO8601Utils.parse(valid_from, ParsePosition(0)))} Uhr"
            }
            MobilityboxTicketValidity.EXPIRED -> {
                "Ticket ist abgelaufen."
            }
        }
    }

    fun getReference(): (String) {
        return if (this.id.matches(Regex("^mobilitybox-ticket-.{36}-[^-]+\$"))) {
            "T-${this.id.replace(Regex("-[^-]+\$"), "").takeLast(6).uppercase()}"
        } else {
            "T-${this.id.takeLast(6).uppercase()}"
        }
    }

    fun validity(): (MobilityboxTicketValidity) {
        if (valid_from == null || valid_until == null) {
            return MobilityboxTicketValidity.EXPIRED
        } else {
            var validFromTime = ISO8601Utils.parse(valid_from, ParsePosition(0))
            var validUntilTime = ISO8601Utils.parse(valid_until, ParsePosition(0))
            return if (validUntilTime.time < System.currentTimeMillis()) {
                MobilityboxTicketValidity.EXPIRED
            } else if (validFromTime.time > System.currentTimeMillis()) {
                MobilityboxTicketValidity.FUTURE
            } else {
                MobilityboxTicketValidity.VALID
            }
        }
    }

    fun reactivate(completion: (ticketCode: MobilityboxTicketCode) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        if (product != null && product.is_subscription && coupon_reactivation_key != null && wasReactivated != null && !wasReactivated!!) {
            MobilityboxCouponCode(coupon_id).fetchCoupon({ fetchedCoupon ->
                if (fetchedCoupon.subscription != null && fetchedCoupon.subscription!!.coupon_reactivatable) {
                    fetchedCoupon.reactivate(coupon_reactivation_key, { fetchedTicketCode ->
                        Log.d("TICKET_REACTIVATION", "reactivated ticket id: ${fetchedTicketCode.ticketId}")
                        completion(fetchedTicketCode)
                        wasReactivated = true
                    }) { mobilityboxError ->
                        if (failure != null) { failure(mobilityboxError) }
                    }
                } else {
                    if (failure != null) { failure(MobilityboxError.NOT_REACTIVATABLE) }
                }
            }) { mobilityboxError ->
                if (failure != null) { failure(mobilityboxError) }
            }
        } else {
            if (failure != null) { failure(MobilityboxError.NOT_REACTIVATABLE) }
        }
    }

    fun getAvailableRenderingOptions(completion: (availableRenderingOptions: ArrayList<String>) -> Unit, failure: ((error: MobilityboxError) -> Unit)? = null) {
        if (this.ticket?.meta?.available_rendering_options != null) {
            completion(this.ticket.meta.available_rendering_options)
        } else {
            this.fetchAvailableRenderingOptions(completion, failure)
        }
    }

    private fun fetchAvailableRenderingOptions(completion: (availableRenderingOptions: ArrayList<String>) -> Unit, failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/tickets/${this.id}/available_rendering_options.json")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (failure != null) {
                    failure(MobilityboxError.UNKOWN)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                if (response.code != 200) {
                    if (failure != null) {
                        failure(MobilityboxError.UNKOWN)
                    }
                } else {
                    val gson = GsonBuilder().create()
                    val availableRenderingOptions = gson.fromJson(body, ArrayList<String>()::class.java)
                    completion(availableRenderingOptions)
                }
            }
        })
    }

    fun getGoogleWalletPassJWT(completion: (jwt: String) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        this.getAvailableRenderingOptions({ availableRenderingOptions ->
            if (availableRenderingOptions.contains("google_wallet")) {
                this.fetchGoogleWalletPassJWT(completion, failure)
            } else {
                if (failure != null) {
                    failure(MobilityboxError.GOOGLE_PASS_NOT_POSSIBLE)
                }
            }
        }, { error ->
            if (failure != null) {
                failure(MobilityboxError.GOOGLE_PASS_NOT_AVAILABLE)
            }
        })
    }

    private fun fetchGoogleWalletPassJWT(completion: (jwt: String) -> (Unit), failure: ((error: MobilityboxError) -> Unit)? = null) {
        val url = URL("${MobilityboxApi.apiUrl}/ticketing/passes/google_wallet/${this.id}.json")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (failure != null) {
                    failure(MobilityboxError.GOOGLE_PASS_NOT_AVAILABLE)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response?.body?.string()
                if (response.code != 200) {
                    if (failure != null) {
                        failure(MobilityboxError.GOOGLE_PASS_NOT_AVAILABLE)
                    }
                } else {
                    if (body != null) {
                        var jwt = body.split("/save/")[1]
                        completion(jwt)
                    } else {
                        if (failure != null) {
                            failure(MobilityboxError.GOOGLE_PASS_NOT_AVAILABLE)
                        }
                    }
                }

            }
        })
    }
}


enum class MobilityboxTicketValidity {
    VALID, FUTURE, EXPIRED
}

data class MobilityboxTicketDetails(
    val meta: MobilityboxTicketMetaDetails?,
    val properties: JsonElement
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(MobilityboxTicketMetaDetails::class.java.classLoader),
        Gson().fromJson(parcel.readString(), JsonElement::class.java)
    ) {
    }

    fun propertiesToString(): (String){
        return Gson().toJson(properties)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(meta, flags)
        parcel.writeString(propertiesToString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MobilityboxTicketDetails> {
        override fun createFromParcel(parcel: Parcel): MobilityboxTicketDetails {
            return MobilityboxTicketDetails(parcel)
        }

        override fun newArray(size: Int): Array<MobilityboxTicketDetails?> {
            return arrayOfNulls(size)
        }
    }
}

@Parcelize
data class MobilityboxTicketMetaDetails(
    val version: String,
    val template: String,
    val requires_engine: String,
    val available_rendering_options: ArrayList<String>?
) : Parcelable
