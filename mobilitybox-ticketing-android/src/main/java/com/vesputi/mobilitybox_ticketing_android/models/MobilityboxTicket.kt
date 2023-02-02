package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import com.google.gson.JsonElement
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.text.SimpleDateFormat
import java.util.Date

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
        return "${this.area?.properties?.city_name} - ${this.product?.getTitle()}"
    }

    fun getDescription(): (String) {
        var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        var formatter = SimpleDateFormat("dd. MMMM, HH:mm")

        return when (validity()) {
            MobilityboxTicketValidity.VALID -> {
                "gültig bis: ${formatter.format(parser.parse(valid_until))} Uhr"
            }
            MobilityboxTicketValidity.FUTURE -> {
                "gültig ab: ${formatter.format(parser.parse(valid_from))} Uhr"
            }
            MobilityboxTicketValidity.EXPIRED -> {
                "Ticket ist abgelaufen."
            }
        }
    }



    fun validity(): (MobilityboxTicketValidity) {
        var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        var validFromTime = parser.parse(valid_from)
        var validUntilTime = parser.parse(valid_until)
        return if (validUntilTime.time < System.currentTimeMillis()) {
            MobilityboxTicketValidity.EXPIRED
        } else if (validFromTime.time > System.currentTimeMillis()) {
            MobilityboxTicketValidity.FUTURE
        } else {
            MobilityboxTicketValidity.VALID
        }
    }

enum class MobilityboxTicketValidity {
    VALID, FUTURE, EXPIRED
}

@Parcelize
class MobilityboxTicketDetails(
    val meta: MobilityboxTicketMetaDetails?,
    val properties: @RawValue JsonElement
) : Parcelable

@Parcelize
data class MobilityboxTicketMetaDetails(
    val version: String,
    val template: String,
    val requires_engine: String
) : Parcelable
