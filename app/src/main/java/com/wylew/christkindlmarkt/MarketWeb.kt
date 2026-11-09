package com.wylew.christkindlmarkt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Builds the WebView that runs assets/index.html.
 *
 * The page is served from https://appassets.androidplatform.net/assets/ through
 * WebViewAssetLoader instead of file://, which keeps it on a normal secure origin
 * (localStorage, canvas and fonts all behave exactly as in a browser).
 */
object MarketWeb {
    private const val HOST = "appassets.androidplatform.net"
    const val PAGE = "https://$HOST/assets/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, bridge: MarketBridge, wallpaper: Boolean): WebView {
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        return WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            if (wallpaper) {
                isFocusable = false
                isClickable = false
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }

            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)

                // Keep our page in the WebView; open any other link in the browser (never from the wallpaper).
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == HOST) return false
                    if (!wallpaper) {
                        runCatching {
                            view.context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    return true
                }
            }

            addJavascriptInterface(bridge, "AndroidBridge")
            loadUrl(if (wallpaper) "$PAGE#wallpaper" else PAGE)
        }
    }
}
