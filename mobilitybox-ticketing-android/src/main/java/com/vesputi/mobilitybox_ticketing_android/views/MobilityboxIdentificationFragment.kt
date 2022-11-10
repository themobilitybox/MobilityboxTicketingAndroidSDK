package com.vesputi.mobilitybox_ticketing_android.views

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.vesputi.mobilitybox_ticketing_android.R
import com.vesputi.mobilitybox_ticketing_android.models.Mobilitybox
import com.vesputi.mobilitybox_ticketing_android.views.MobilityboxBottomSheetFragment
import com.google.gson.GsonBuilder
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxCoupon
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxIdentificationMedium
import com.vesputi.mobilitybox_ticketing_android.models.MobilityboxTicketCode

class MobilityboxIdentificationFragment : Fragment() {
    lateinit var identificationView: WebView
    private var coupon: MobilityboxCoupon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            coupon = it.get("coupon") as MobilityboxCoupon?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mobilitybox_identification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        WebView.setWebContentsDebuggingEnabled(true)
        identificationView = view.findViewById(R.id.webView)

        with(identificationView)  {
            settings.javaScriptEnabled = true
            settings.setDomStorageEnabled(true)

            loadDataWithBaseURL("about:blank",
                Mobilitybox.identificationViewEngine.engineString!!, "text/html", "utf-8", null)

            addJavascriptInterface(IdentificationWebViewEventHandler(this.context), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val gson = GsonBuilder().create()
                    val identification_medium_schema = gson.toJson(coupon!!.product.identification_medium_schema)

                    view?.evaluateJavascript("window.renderIdentificationView(${identification_medium_schema})", null)

                    view?.evaluateJavascript("""
                        document.getElementById('submit_activate_button').addEventListener('click', function(e){
                            const identification_medium = window.getIdentificationMedium()
                            if (identification_medium != undefined) {
                                window.Android.activateCoupon(identification_medium);
                            }
                        })
                    """, null)
                    view?.evaluateJavascript("""
                        document.getElementById('close_identification_form_button').addEventListener('click', function(e){
                            window.Android.closeView();
                        })
                    """, null)


                    super.onPageFinished(view, url)
                }

                @SuppressWarnings("deprecation")
                override fun shouldOverrideUrlLoading(view: WebView?, requestUrl: String?): Boolean {
                    var uri = Uri.parse(requestUrl);
                    return handleUri(uri);
                }


                @TargetApi(Build.VERSION_CODES.N)
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    var uri = Uri.parse(request?.url.toString())
                    return handleUri(uri)
                }

                fun handleUri(uri: Uri): Boolean {
                    Log.d("URL-OVERRIDE", uri.toString())

                    if (uri.host == "about:blank") {
                        return false
                    }

                    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage("com.android.chrome")
                        try {
                            context.startActivity(this)
                        } catch (ex: ActivityNotFoundException) {
                            // Chrome browser presumably not installed so allow user to choose instead
                            setPackage(null)
                            context.startActivity(this)
                        }
                    }

                    return true
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(coupon: MobilityboxCoupon) = MobilityboxIdentificationFragment().apply {
            arguments = Bundle().apply {
                putParcelable("coupon", coupon)
            }
        }
    }

    fun activateCouponCompletion(ticketCode: MobilityboxTicketCode) {
        Log.d("RECEIVED_TICKET_CODE", ticketCode.ticketId)
        (parentFragment as MobilityboxBottomSheetFragment).activateCouponCallback(ticketCode)
    }

    private inner class IdentificationWebViewEventHandler(private val mContext: Context) {

        @JavascriptInterface
        fun activateCoupon(data: String) {
            Log.d("DEBUG_activeCoupon", data)
            if (coupon != null) {
                coupon?.activate(MobilityboxIdentificationMedium(data), ::activateCouponCompletion)
            }
        }

        @JavascriptInterface
        fun closeView() {
            Log.d("DEBUG_closeView", "CLOSE")
            (parentFragment as MobilityboxBottomSheetFragment).close()
        }

        @JavascriptInterface
        fun logData(data: String) {
            Log.d("DEBUG_LOG_WEBVIEW", data)
        }
    }
}