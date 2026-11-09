package com.wylew.christkindlmarkt

import android.app.Presentation
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.WindowManager
import android.webkit.WebView

/**
 * Live wallpaper that renders the HTML scene.
 *
 * A WebView can't draw straight into a wallpaper Surface, so each engine creates a
 * private VirtualDisplay backed by the wallpaper's Surface and shows a Presentation
 * (a window on that display) whose content is the WebView. The WebView then renders
 * with normal hardware acceleration at the display's refresh rate — that's what keeps
 * the sub-pixel drift smooth — and nothing extra is copied per frame.
 */
class MarketWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MarketEngine()

    private inner class MarketEngine : Engine() {
        private var display: VirtualDisplay? = null
        private var presentation: Presentation? = null
        private var web: WebView? = null
        private var bridge: MarketBridge? = null
        /** The saved-state version the page last loaded; -1 until the scene exists. */
        private var loadedVersion = -1L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            setOffsetNotificationsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (width <= 0 || height <= 0) return
            val dpi = resources.displayMetrics.densityDpi
            val current = display
            if (current == null) {
                val dm = getSystemService(DisplayManager::class.java)
                // flags = 0 → a private display only this app can put windows on; no permission needed.
                display = dm.createVirtualDisplay("christkindlmarkt-wallpaper", width, height, dpi, holder.surface, 0)
                showScene()
            } else {
                current.surface = holder.surface
                current.resize(width, height, dpi)
            }
        }

        private fun showScene() {
            val vd = display ?: return
            val p = Presentation(this@MarketWallpaperService, vd.display, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val b = MarketBridge(this@MarketWallpaperService, wallpaper = true)
            // Read before the page boots: if a save lands in between, the page has the newer state and we reload once more, harmlessly.
            loadedVersion = b.version()
            val view = MarketWeb.create(p.context, b, wallpaper = true)
            bridge = b
            p.setContentView(view)
            p.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.BLACK))
                addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            }
            p.show()
            presentation = p
            web = view
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            val view = web ?: return
            if (visible) {
                view.onResume()
                // Reload only if the builder saved since we last loaded: reloading drops every sprite and sky cache,
                // which made each unlock / return home a CPU spike.
                val v = bridge?.version() ?: loadedVersion
                val reload = v != loadedVersion
                loadedVersion = v
                view.evaluateJavascript(if (reload) "window.market&&(market.reload(),market.setPaused(false))" else "window.market&&market.setPaused(false)", null)
            } else {
                view.evaluateJavascript("window.market&&market.setPaused(true)", null)
                view.onPause()
            }
        }

        /** Home-screen page swipes. The page only follows them when "Home swipes → Follow" is set. */
        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            web?.evaluateJavascript("window.setWallpaperOffset&&setWallpaperOffset($xOffset)", null)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            tearDown()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            tearDown()
            super.onDestroy()
        }

        private fun tearDown() {
            web?.let {
                it.evaluateJavascript("window.market&&market.setPaused(true)", null)
                it.stopLoading()
                it.destroy()
            }
            web = null
            bridge = null
            presentation?.dismiss()
            presentation = null
            display?.release()
            display = null
        }
    }
}
