package com.vesputi.mobilitybox_ticketing_android.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MobilityboxSubscription (
    val id: String,
    val active: Boolean,
    val coupon_reactivatable: Boolean,
    val current_cycle_valid_from: String?,
    val current_cycle_valid_until: String?,
    val ordered_until: String?,
    val current_subscription_cycle: MobilityboxSubscriptionCycle,
    val next_subscription_cycle: MobilityboxSubscriptionCycle?,
    val next_unordered_subscription_cycle: MobilityboxSubscriptionCycle?,
) : Parcelable

@Parcelize
class MobilityboxSubscriptionCycle (
    val id: String,
    val valid_from: String?,
    val valid_until: String?,
    val ordered: Boolean,
    val coupon_activated: Boolean,
): Parcelable