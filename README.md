# Christkindlmarkt — Android live wallpaper

A pixel-art Bavarian Christmas market you build yourself, then set as a live wallpaper.
The whole scene is one HTML/canvas page (`app/src/main/assets/index.html`, the same page as the web version);
the Android side hosts it twice: once as the builder app and once inside the wallpaper.

## Open & run
1. Android Studio → **Open** → this folder. Let Gradle sync (it downloads Gradle 8.9, AGP and the AndroidX libraries).
   If Studio offers to upgrade AGP/Kotlin/library versions, accepting is fine.
2. Run the **app** configuration on a device or emulator (API 26+).
3. Build your market, then tap **📱 Set wallpaper** in the top bar → **Set wallpaper** in the system preview.

## How it fits together
| File | Role |
|---|---|
| `assets/index.html` | The scene, builder UI and all pixel art. Single source of truth — edit here, then copy to the web artifact if you keep one. |
| `MainActivity.kt` | The builder: full-screen WebView, edge-to-edge insets, back-button handling, opens the live-wallpaper preview. |
| `MarketWallpaperService.kt` | The wallpaper. Creates a private `VirtualDisplay` on the wallpaper's `Surface` and shows a `Presentation` containing the WebView, so it renders hardware-accelerated at full refresh rate (smooth sub-pixel drift). Pauses when hidden, reloads settings when shown, forwards home-screen offsets. |
| `MarketWeb.kt` | Builds the WebView and serves the page from `https://appassets.androidplatform.net/assets/` via `WebViewAssetLoader`. |
| `MarketBridge.kt` | `window.AndroidBridge` — `save/load` the market JSON in SharedPreferences (shared by app and wallpaper), `isWallpaper()`, `setWallpaper()`. |

### Page ↔ Android contract (already implemented in index.html)
- `AndroidBridge.save(json)` / `load()` — persistence; falls back to `localStorage` in a browser.
- `AndroidBridge.isWallpaper()` → page adds `body.svc`, hides every control and starts in wallpaper mode.
- `AndroidBridge.canSetWallpaper()` → page adds the **Set wallpaper** button.
- `market.setPaused(bool)`, `market.reload()`, `market.back()` — lifecycle/back hooks called from Kotlin.
- `setWallpaperOffset(0..1)` — home-screen swipes; only used when **⚙ → Home swipes → Follow** is chosen (default is auto drift at your chosen speed).

## Notes & next steps
- **Battery:** the wallpaper renders every display frame while visible and fully stops when hidden (`setPaused` + `WebView.onPause`). A frame-rate cap setting would be an easy addition if needed.
- **Fonts:** the builder UI pulls Pixelify Sans / Silkscreen from Google Fonts (hence `INTERNET`). For a fully offline app, drop the `.woff2` files into `assets/fonts/` and switch the `<link>` to a local `@font-face`.
- **Testing the service quickly:** Settings → Wallpaper & style → Live wallpapers, or `adb shell am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER --ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT com.wylew.christkindlmarkt/.MarketWallpaperService`.
