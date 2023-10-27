package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MobilityboxTariffSettings(val tariffSettingsJson: String) : Parcelable
