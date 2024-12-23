package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class MobilityboxArea(
    var id: String,
    var type: String,
    var properties: MobilityboxAreaProperties,
    var geometry: MobilityboxAreaGeometry
) : Parcelable

@Parcelize
data class MobilityboxAreaProperties(
    var city_name: String,
    var local_zone_name: String
) : Parcelable


data class MobilityboxAreaGeometry(
    var type: String,
    var coordinates: JsonElement
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        Gson().fromJson(parcel.readString(), JsonElement::class.java)
    ) {
    }

    fun coordinatesToString(): (String) {
        return Gson().toJson(coordinates)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(coordinatesToString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MobilityboxAreaGeometry> {
        override fun createFromParcel(parcel: Parcel): MobilityboxAreaGeometry {
            return MobilityboxAreaGeometry(parcel)
        }

        override fun newArray(size: Int): Array<MobilityboxAreaGeometry?> {
            return arrayOfNulls(size)
        }
    }
}

@Parcelize
data class MobilityboxTicketArea(
    val id: String,
    var type: String?,
    val properties: MobilityboxTicketAreaProperties,
    var geometry: MobilityboxAreaGeometry?
) : Parcelable

@Parcelize
class MobilityboxTicketAreaProperties(
    val city_name: String,
    val local_zone_name: String,
    val relation_number: String?,
    val price_level: String?
) : Parcelable