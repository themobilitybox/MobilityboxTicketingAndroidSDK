package com.vesputi.mobilitybox_ticketing_android.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vesputi.mobilitybox_ticketing_android.R
import com.vesputi.mobilitybox_ticketing_android.models.Mobilitybox
import com.google.gson.GsonBuilder
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxTicket

class MobilityboxTicketInspectionFragment : Fragment() {
    lateinit var ticketInspectionView: WebView
    private var ticket: MobilityboxTicket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ticket = it.get("ticket") as MobilityboxTicket?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mobilitybox_ticket_inspection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        WebView.setWebContentsDebuggingEnabled(true)
        ticketInspectionView = view.findViewById(R.id.webView)

        with(ticketInspectionView)  {
            settings.javaScriptEnabled = true
            settings.setDomStorageEnabled(true)

            loadDataWithBaseURL("about:blank", Mobilitybox.renderingEngine.engineString!!, "text/html", "utf-8", null)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (ticket != null) {
                        val gson = GsonBuilder().create()
                        val ticketData = gson.toJson(ticket, MobilityboxTicket::class.java)

                        view?.evaluateJavascript("window.renderTicket(${ticketData})", null)
                    }
                    super.onPageFinished(view, url)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(ticket: MobilityboxTicket) =
            MobilityboxTicketInspectionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("ticket", ticket)
                }
            }
    }
}