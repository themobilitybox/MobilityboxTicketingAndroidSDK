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
import android.webkit.*
import androidx.fragment.app.Fragment
import com.vesputi.mobilitybox_ticketing_android.R
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.internal.bind.util.ISO8601Utils
import com.vesputi.mobilitybox_ticketing_android.models.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.text.ParsePosition
import java.util.*
import java.util.concurrent.CountDownLatch

class MobilityboxIdentificationFragment : Fragment() {
    lateinit var identificationView: WebView
    private var coupon: MobilityboxCoupon? = null
    private var ticket: MobilityboxTicket? = null
    private var product: MobilityboxProduct? = null
    private var activationStartDateTime: Date? = null
    var activationRunning: Boolean = false
    private var pageFinished: Boolean = false
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            coupon = it.get("coupon") as MobilityboxCoupon?
            ticket = it.get("ticket") as MobilityboxTicket?
            val activationStartDateTimeString = it.get("activationStartDateTime") as String?
            if (activationStartDateTimeString != null) {
                activationStartDateTime = ISO8601Utils.parse(activationStartDateTimeString, ParsePosition(0))
            }

            if (ticket != null) {
                val reactivatableCycle = coupon?.subscription?.subscription_cycles?.first { cycle ->
                    cycle.ordered && !cycle.coupon_activated
                }

                if (reactivatableCycle?.product_id != null && reactivatableCycle.product_id != coupon?.product?.id) {
                    MobilityboxProductCode(reactivatableCycle.product_id).fetchProduct({ fetchedProduct ->
                        this.product = fetchedProduct
                        this.activity?.runOnUiThread {
                            loadWebView()
                        }
                    }) {
                        this.product = coupon?.product
                        this.activity?.runOnUiThread {
                            loadWebView()
                        }
                    }
                } else {
                    product = coupon?.product
                    this.activity?.runOnUiThread {
                        loadWebView()
                    }
                }
            } else {
                product = coupon?.product
                this.activity?.runOnUiThread {
                    loadWebView()
                }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                settings.forceDark = WebSettings.FORCE_DARK_OFF
            }

            loadDataWithBaseURL("about:blank",
                Mobilitybox.identificationViewEngine.engineString!!, "text/html", "utf-8", null)

            addJavascriptInterface(IdentificationWebViewEventHandler(this.context), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    webView = view
                    if (product != null) {
                        loadWebView()
                    }
                    pageFinished = true
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

    fun loadWebView() {
        val gson = GsonBuilder().create()
        val identificationMediumSchema = gson.toJson(product!!.identification_medium_schema)
        val tariffSettingsSchema = gson.toJson(product!!.tariff_settings_schema)
        var identificationMedium = "null"
        var tariffSettings = "null"

        if (ticket != null && this.ticket?.ticket?.properties != null) {
            var properties = JSONObject(this.ticket!!.ticket!!.properties.toString())
            identificationMedium = properties.getString("identification_medium")
        }


        webView?.evaluateJavascript("window.renderIdentificationView(${identificationMediumSchema}, ${tariffSettingsSchema}, ${identificationMedium}, ${tariffSettings}, true)", null)

        webView?.evaluateJavascript("""
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
        webView?.evaluateJavascript("""
                        document.getElementById('close_identification_form_button').addEventListener('click', function(e){
                            window.Android.closeView();
                        })
                    """, null)

        webView?.evaluateJavascript("""
                        Array.from(document.getElementsByTagName('input')).forEach(function(input){
                            input.addEventListener('focus', function(e){
                                window.Android.focus()
                            })
                        })
                    """, null)
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

        fun newInstance(coupon: MobilityboxCoupon, activationStartDateTime: Date? = null, ticket: MobilityboxTicket? = null) = MobilityboxIdentificationFragment().apply {
            arguments = Bundle().apply {
                putParcelable("coupon", coupon)
                putParcelable("ticket", ticket)
                if (activationStartDateTime != null) {
                    putString("activationStartDateTime", ISO8601Utils.format(activationStartDateTime).toString())
                }
            }
        }
    }

    fun activateCouponCompletion(ticketCode: MobilityboxTicketCode) {
        Log.d("RECEIVED_TICKET_CODE", ticketCode.ticketId)
        (parentFragment as MobilityboxBottomSheetFragment).activateCouponCallback(ticketCode)
        activationRunning = false
    }

    fun activateCouponFailure(mobilityboxError: MobilityboxError) {
        Log.e("IDENTICATION_VIEW", "Error while activation coupon: ${mobilityboxError.toString()}")
        (parentFragment as MobilityboxBottomSheetFragment).activateCouponFailure(mobilityboxError)
        activationRunning = false
    }

    fun reactivateTicketCompletion(ticketCode: MobilityboxTicketCode) {
        Log.d("RECEIVED_TICKET_CODE", ticketCode.ticketId)
        (parentFragment as MobilityboxBottomSheetFragment).reactivateTicketCompletion(ticketCode)
        activationRunning = false
    }

    fun reactivateTicketFailure(mobilityboxError: MobilityboxError) {
        Log.e("IDENTICATION_VIEW", "Error while reactivate coupon: ${mobilityboxError.toString()}")
        (parentFragment as MobilityboxBottomSheetFragment).reactivateTicketFailure(mobilityboxError)
        activationRunning = false
    }

    private inner class IdentificationWebViewEventHandler(private val mContext: Context) {

        @JavascriptInterface
        fun activateCoupon(identififcationMediumData: String?, tariffSettingsData: String?) {
            Log.d("DEBUG_activeCoupon", "identificationMediumData: $identififcationMediumData")
            Log.d("DEBUG_activeCoupon", "tariffSettingsData: $tariffSettingsData")
            if (coupon != null && !activationRunning) {
                val identificationMediumAndTariffsettingsValid = (identififcationMediumData != null && tariffSettingsData != null)
                val onlyIdentificationMediumValid = (identififcationMediumData != null && tariffSettingsData == null)
                val onlyTariffSettingsValid = (identififcationMediumData == null && tariffSettingsData != null)

                if (identificationMediumAndTariffsettingsValid) {
                    activationRunning = true

                    if (ticket != null) {
                        coupon?.reactivate(
                            ticket?.coupon_reactivation_key ?: "",
                            MobilityboxIdentificationMedium(identififcationMediumData!!),
                            MobilityboxTariffSettings(tariffSettingsData!!),
                            ::reactivateTicketCompletion,
                            ::reactivateTicketFailure
                        )
                    } else {
                        coupon?.activate(
                            MobilityboxIdentificationMedium(identififcationMediumData!!),
                            MobilityboxTariffSettings(tariffSettingsData!!),
                            ::activateCouponCompletion,
                            activationStartDateTime,
                            ::activateCouponFailure
                        )
                    }
                } else if (onlyIdentificationMediumValid) {
                    activationRunning = true
                    if (ticket != null) {
                        coupon?.reactivate(
                            ticket?.coupon_reactivation_key ?: "",
                            MobilityboxIdentificationMedium(identififcationMediumData!!),
                            null,
                            ::reactivateTicketCompletion,
                            ::reactivateTicketFailure
                        )
                    } else {
                        coupon?.activate(
                            MobilityboxIdentificationMedium(identififcationMediumData!!),
                            ::activateCouponCompletion,
                            activationStartDateTime,
                            ::activateCouponFailure
                        )
                    }
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
            (parentFragment as MobilityboxBottomSheetFragment).disableDragBottomSheetCallback()
        }

        @JavascriptInterface
        fun logData(data: String) {
            Log.d("DEBUG_LOG_WEBVIEW", data)
        }
    }
}