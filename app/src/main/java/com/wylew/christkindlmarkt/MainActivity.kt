package com.wylew.christkindlmarkt

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** The market builder: the page in build mode, plus a native "Set wallpaper" hook. */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val bridge = MarketBridge(this, wallpaper = false, onSetWallpaper = ::openWallpaperPicker)
        web = MarketWeb.create(this, bridge, wallpaper = false)

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF05060F.toInt())
            addView(web, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        // targetSdk 35 is edge-to-edge: pad the WebView so the page's top bar and bottom panel clear the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)

        // Back closes things inside the page first (preview, settings, selection), then leaves the app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.market&&market.back)?market.back():false") { handled ->
                    if (handled != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    /** Opens the system preview for our wallpaper, falling back to the generic chooser. */
    private fun openWallpaperPicker() {
        val component = ComponentName(this, MarketWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        try {
            startActivity(direct)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                Toast.makeText(this, "Pick “Christkindlmarkt” from the list", Toast.LENGTH_LONG).show()
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, "This device has no live wallpaper picker", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.evaluateJavascript("window.market&&market.setPaused(false)", null)
    }

    override fun onPause() {
        web.evaluateJavascript("window.market&&market.setPaused(true)", null)
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
