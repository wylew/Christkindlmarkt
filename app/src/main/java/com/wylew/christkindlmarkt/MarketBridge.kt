package com.wylew.christkindlmarkt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Exposed to the page as `window.AndroidBridge`.
 *
 * The builder (MainActivity) and the wallpaper run in the same process, so one
 * SharedPreferences entry is the single source of truth for the market layout
 * and settings. The page writes JSON here on every change and reads it back on load.
 */
class MarketBridge(
    context: Context,
    private val wallpaper: Boolean,
    private val onSetWallpaper: (() -> Unit)? = null,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun save(json: String) {
        prefs.edit().putString(KEY_STATE, json).apply()
    }

    /** Returns "" when nothing is saved yet (the page then builds the sample market). */
    @JavascriptInterface
    fun load(): String = prefs.getString(KEY_STATE, "") ?: ""

    /** True inside the wallpaper service: the page hides all controls and starts in wallpaper mode. */
    @JavascriptInterface
    fun isWallpaper(): Boolean = wallpaper

    /** True in the builder: the page adds a "Set wallpaper" button to its top bar. */
    @JavascriptInterface
    fun canSetWallpaper(): Boolean = onSetWallpaper != null

    @JavascriptInterface
    fun setWallpaper() {
        onSetWallpaper?.let { main.post(it) }
    }

    companion object {
        const val PREFS = "market"
        const val KEY_STATE = "state"
    }
}
