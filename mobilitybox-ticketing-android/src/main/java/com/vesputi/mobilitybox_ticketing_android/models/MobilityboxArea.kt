package com.mobilitybox.android.models

import android.os.Parcelable
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

@Parcelize
data class MobilityboxAreaGeometry(
    var type: String,
    var coordinates: @RawValue JsonElement
) : Parcelable

@Parcelize
data class MobilityboxTicketArea(
    val id: String,
    val properties: MobilityboxTicketAreaProperties
) : Parcelable

@Parcelize
class MobilityboxTicketAreaProperties(
    val city_name: String,
    val local_zone_name: String,
    val geojson: @RawValue JsonElement?
) : Parcelable