package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import com.google.gson.JsonElement
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class MobilityboxProduct(
    var id: String,
    var local_ticket_name: String,
    var local_validity_description: String,
    var ticket_type: String,
    var customer_type: String,
    var price_in_cents: Int,
    var currency: String,
    var validity_in_minutes: Int,
    var area_id: String,
    var identification_medium_schema: @RawValue JsonElement
): Parcelable {
    fun getTitle(): (String) {
        val customer_type_string = if (customer_type == "adult") " Erwachsener" else ( if (customer_type == "child") " Kind" else "")
        val ticket_type_string = if (ticket_type == "single") "Einzelticket" else ( if (ticket_type == "day") "Tagesticket" else "")
        return "${ticket_type_string}${customer_type_string}"
    }

    fun getDescription(): (String) {
        val validity_time_string = if (validity_in_minutes > 90) "${validity_in_minutes / 60} Stunden" else "${validity_in_minutes} Minuten"
        return "Dieses Ticket ist nach dem Entwerten ${validity_time_string} gültig."
    }
}

@Parcelize
data class MobilityboxOrderedProduct(
    val id: String,
    val area_id: String,
    val local_ticket_name: String,
    val local_validity_description: String,
    val ticket_type: String,
    val customer_type: String,
    val price_in_cents: Int,
    val currency: String,
    val validity_in_minutes: Int
) : Parcelable {
    fun getTitle(): (String) {
        val customer_type_string = if (customer_type == "adult") " Erwachsener" else ( if (customer_type == "child") " Kind" else "")
        val ticket_type_string = if (ticket_type == "single") "Einzelticket" else ( if (ticket_type == "day") "Tagesticket" else "")
        return "${ticket_type_string}${customer_type_string}"
    }
}
