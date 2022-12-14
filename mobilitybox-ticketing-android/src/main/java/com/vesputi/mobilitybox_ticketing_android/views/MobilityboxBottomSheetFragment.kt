package com.vesputi.mobilitybox_ticketing_android.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import com.vesputi.mobilitybox_ticketing_android.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxCoupon
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxTicket
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxTicketCode

class MobilityboxBottomSheetFragment : BottomSheetDialogFragment() {
    private var coupon: MobilityboxCoupon? = null
    private var ticket: MobilityboxTicket? = null
    private var ticketElementId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            coupon = it.get("coupon") as MobilityboxCoupon?
            ticket = it.get("ticket") as MobilityboxTicket?
            ticketElementId = it.get("ticketElementId") as String?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mobilitybox_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        childFragmentManager.beginTransaction().apply {
            if (coupon != null) {
                val identificationView = MobilityboxIdentificationFragment.newInstance(coupon!!)
                replace(R.id.flWebViewFragment, identificationView)
            } else if (ticket != null) {
                val ticketInspectionView = MobilityboxTicketInspectionFragment.newInstance(ticket!!)
                replace(R.id.flWebViewFragment, ticketInspectionView)
            }
            commit()
        }

        val closeSheetButton = view.findViewById<ImageView>(R.id.closeSheetButton)
        closeSheetButton.setOnClickListener {
            dismiss()
        }
    }

    fun activateCouponCallback(ticketCode: MobilityboxTicketCode) {
        activity?.supportFragmentManager?.setFragmentResult("activateCoupon", bundleOf("ticketCode" to ticketCode, "ticketElementId" to ticketElementId))
        dismiss()
    }

    fun close() {
        dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(coupon: MobilityboxCoupon, ticketElementId: String) = MobilityboxBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable("coupon", coupon)
                putString("ticketElementId", ticketElementId)
            }
        }

        @JvmStatic
        fun newInstance(ticket: MobilityboxTicket, ticketElementId: String) = MobilityboxBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable("ticket", ticket)
                putString("ticketElementId", ticketElementId)
            }
        }
    }
}