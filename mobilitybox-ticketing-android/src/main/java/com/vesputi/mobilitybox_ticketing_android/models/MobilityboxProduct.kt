package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

data class MobilityboxProduct(
    var id: String,
    val recommended_successor_is: String?,
    val recommended_successor_of: String?,
    var local_ticket_name: String,
    var local_validity_description: String,
    var ticket_type: String,
    var customer_type: String,
    var price_in_cents: Int,
    var currency: String,
    val duration_definition: String,
    val duration_in_minutes: Int?,
    var validity_in_minutes: Int?,
    var area_id: String,
    var is_subscription: Boolean,
    var identification_medium_schema: JsonElement
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        Gson().fromJson(parcel.readString(), JsonElement::class.java)
    ) {
    }

    fun getTitle(): (String) {
        val customer_type_string = if (customer_type == "adult") " Erwachsener" else ( if (customer_type == "child") " Kind" else "")
        val ticket_type_string = if (ticket_type == "single") "Einzelticket" else ( if (ticket_type == "day") "Tagesticket" else "")
        return "${ticket_type_string}${customer_type_string}"
    }

    fun getDescription(): (String) {
        when (duration_definition) {
            "duration_in_minutes" -> {
                return if (duration_in_minutes != null) {
                    val validity_time_string = if (duration_in_minutes > 90) "${duration_in_minutes / 60} Stunden" else "${duration_in_minutes} Minuten"
                    "Dieses Ticket ist nach dem Aktivieren ${validity_time_string} gültig."
                } else {
                    ""
                }
            }
            else -> return ""
        }
    }

    fun identificationMediumSchemaToString(): (String){
        return Gson().toJson(identification_medium_schema)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(recommended_successor_is)
        parcel.writeString(recommended_successor_of)
        parcel.writeString(local_ticket_name)
        parcel.writeString(local_validity_description)
        parcel.writeString(ticket_type)
        parcel.writeString(customer_type)
        parcel.writeInt(price_in_cents)
        parcel.writeString(currency)
        parcel.writeString(duration_definition)
        parcel.writeValue(duration_in_minutes)
        parcel.writeValue(validity_in_minutes)
        parcel.writeString(area_id)
        parcel.writeByte(if (is_subscription) 1 else 0)
        parcel.writeString(identificationMediumSchemaToString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MobilityboxProduct> {
        override fun createFromParcel(parcel: Parcel): MobilityboxProduct {
            return MobilityboxProduct(parcel)
        }

        override fun newArray(size: Int): Array<MobilityboxProduct?> {
            return arrayOfNulls(size)
        }
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
    val duration_definition: String,
    val duration_in_minutes: Int?,
    val validity_in_minutes: Int,
    val is_subscription: Boolean,
) : Parcelable {
    fun getTitle(): (String) {
        val customer_type_string = if (customer_type == "adult") " Erwachsener" else ( if (customer_type == "child") " Kind" else "")
        val ticket_type_string = if (ticket_type == "single") "Einzelticket" else ( if (ticket_type == "day") "Tagesticket" else "")
        return "${ticket_type_string}${customer_type_string}"
    }
}
