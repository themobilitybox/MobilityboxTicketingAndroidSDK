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
import com.google.gson.GsonBuilder
import com.google.gson.internal.bind.util.ISO8601Utils
import com.vesputi.mobilitybox_ticketing_android.models.*
import java.text.ParsePosition
import java.util.*

class MobilityboxIdentificationFragment : Fragment() {
    lateinit var identificationView: WebView
    private var coupon: MobilityboxCoupon? = null
    private var activationStartDateTime: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            coupon = it.get("coupon") as MobilityboxCoupon?
            val activationStartDateTimeString = it.get("activationStartDateTime") as String?
            if (activationStartDateTimeString != null) {
                activationStartDateTime = ISO8601Utils.parse(activationStartDateTimeString, ParsePosition(0))
            }
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
                    val identificationMediumSchema = gson.toJson(coupon!!.product.identification_medium_schema)
                    val tariffSettingsSchema = gson.toJson(coupon!!.product.tariff_settings_schema)

                    view?.evaluateJavascript("window.renderIdentificationView(${identificationMediumSchema}, ${tariffSettingsSchema})", null)

                    view?.evaluateJavascript("""
                        document.getElementById('submit_activate_button').addEventListener('click', function(e){
                            var identification_medium = window.getIdentificationMedium()
                            var tariff_settings = null
                            if (window.getTariffSettings != undefined) {
                                tariff_settings = window.getTariffSettings()
                            }
                            if (identification_medium != undefined || identification_medium != null) {
                                window.Android.activateCoupon(identification_medium, tariff_settings);
                            }
                        })
                    """, null)
                    view?.evaluateJavascript("""
                        document.getElementById('close_identification_form_button').addEventListener('click', function(e){
                            window.Android.closeView();
                        })
                    """, null)

                    view?.evaluateJavascript("""
                        Array.from(document.getElementsByTagName('input')).forEach(function(input){
                            input.addEventListener('focus', function(e){
                                window.Android.focus()
                            })
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
        @JvmStatic @JvmOverloads
        fun newInstance(coupon: MobilityboxCoupon, activationStartDateTime: Date? = null) = MobilityboxIdentificationFragment().apply {
            arguments = Bundle().apply {
                putParcelable("coupon", coupon)
                if (activationStartDateTime != null) {
                    putString("activationStartDateTime", ISO8601Utils.format(activationStartDateTime).toString())
                }
            }
        }
    }

    fun activateCouponCompletion(ticketCode: MobilityboxTicketCode) {
        Log.d("RECEIVED_TICKET_CODE", ticketCode.ticketId)
        (parentFragment as MobilityboxBottomSheetFragment).activateCouponCallback(ticketCode)
    }

    fun activateCouponFailure(mobilityboxError: MobilityboxError) {
        Log.e("IDENTICATION_VIEW", "Error while activation coupon: ${mobilityboxError.toString()}")
        (parentFragment as MobilityboxBottomSheetFragment).activateCouponFailure()
    }

    private inner class IdentificationWebViewEventHandler(private val mContext: Context) {

        @JavascriptInterface
        fun activateCoupon(identififcationMediumData: String?, tariffSettingsData: String?) {
            Log.d("DEBUG_activeCoupon", "identificationMediumData: $identififcationMediumData")
            Log.d("DEBUG_activeCoupon", "tariffSettingsData: $tariffSettingsData")
            if (coupon != null) {
                val identificationMediumAndTariffsettingsValid = (identififcationMediumData != null && tariffSettingsData != null)
                val onlyIdentificationMediumValid = (identififcationMediumData != null && tariffSettingsData == null)
                val onlyTariffSettingsValid = (identififcationMediumData == null && tariffSettingsData != null)

                if (identificationMediumAndTariffsettingsValid) {
                    coupon?.activate(
                        MobilityboxIdentificationMedium(identififcationMediumData!!),
                        MobilityboxTariffSettings(tariffSettingsData!!),
                        ::activateCouponCompletion,
                        activationStartDateTime,
                        ::activateCouponFailure
                    )
                } else if (onlyIdentificationMediumValid) {
                    coupon?.activate(
                        MobilityboxIdentificationMedium(identififcationMediumData!!),
                        ::activateCouponCompletion,
                        activationStartDateTime,
                        ::activateCouponFailure
                    )
                } else if (onlyTariffSettingsValid) {
                    Log.d("DEBUG_activeCoupon", "should update tariff settings only")
                }
            }
        }

        @JavascriptInterface
        fun closeView() {
            Log.d("DEBUG_closeView", "CLOSE")
            (parentFragment as MobilityboxBottomSheetFragment).close()
        }

        @JavascriptInterface
        fun focus() {
            Log.d("DEBUG_focus", "FOCUS")
            (parentFragment as MobilityboxBottomSheetFragment).expandBottomSheetCallback()
        }

        @JavascriptInterface
        fun logData(data: String) {
            Log.d("DEBUG_LOG_WEBVIEW", data)
        }
    }
}