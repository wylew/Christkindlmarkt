# Keep the JavaScript bridge methods callable from the WebView.
-keepclassmembers class com.wylew.christkindlmarkt.MarketBridge {
    @android.webkit.JavascriptInterface <methods>;
}
