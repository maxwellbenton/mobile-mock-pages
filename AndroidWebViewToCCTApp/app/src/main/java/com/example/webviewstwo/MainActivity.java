package com.example.webviewstwo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.webkit.ConsoleMessage;
import android.net.http.SslError;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        webView = findViewById(R.id.webView);

        // Edge-to-edge: handle insets ourselves (status/nav + IME)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int bottom = Math.max(imeBottom, sysBottom);
            v.setPadding(0, 0, 0, bottom);
            // return the insets unconsumed so others can react
            return insets;
        });

        WebSettings s = webView.getSettings();
        
        // Core JavaScript and DOM settings
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        
        // Cache settings - use NO_CACHE for development to avoid stale components
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        // UI settings
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        
        // Network and security settings for external script loading
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // Enhanced settings for modern web components
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setSupportMultipleWindows(true);
        
        // Enable media playback (some components may use audio/video)
        s.setMediaPlaybackRequiresUserGesture(false);
        
        // Enhanced user agent for better compatibility
        String userAgent = s.getUserAgentString();
        s.setUserAgentString(userAgent + " WebViewApp/1.0 CustomComponents/1.0");

        webView.setWebViewClient(new WebViewClient() {
            private boolean hasReloaded = false; // Track if we've already reloaded
            
            // Modern error handling (API 23+)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                android.util.Log.e("WebView", "Error loading " + request.getUrl() + ": " + 
                    error.getDescription() + " (Code: " + error.getErrorCode() + ")");
            }
            
            // Legacy error handling (for compatibility with older Android versions)
            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                android.util.Log.e("WebView", "Error loading " + failingUrl + ": " + description + " (Code: " + errorCode + ")");
            }
            
            // Log when pages start and finish loading
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                android.util.Log.d("WebView", "Started loading: " + url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.util.Log.d("WebView", "Finished loading: " + url);
                
                // Reload the page once when it first loads
                if (!hasReloaded) {
                    hasReloaded = true;
                    android.util.Log.d("WebView", "Reloading page on first load");
                    view.postDelayed(() -> {
                        view.reload();
                    }, 500); // Small delay to ensure page is fully loaded before reload
                }
            }
            
            // Allow all SSL certificates for development (be careful in production!)
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                android.util.Log.w("WebView", "SSL Error: " + error.toString());
                handler.proceed(); // Accept all SSL certificates for development
            }
        });
        
        // Enhanced WebChromeClient for better custom component support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("WebConsole", "[" + consoleMessage.messageLevel() + "] " + 
                    consoleMessage.message() + " -- From line " + 
                    consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                android.util.Log.d("WebView", "Loading progress: " + newProgress + "%");
            }
            
            // Support for file input (if components need file access)
            @Override
            public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<android.net.Uri[]> filePathCallback, 
                    FileChooserParams fileChooserParams) {
                android.util.Log.d("WebView", "File chooser requested");
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });
        
        // Add JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        
        // Choose ONE of these URLs to test:
        
        // 1. Local HTML file (always works):
//        webView.loadUrl("file:///android_asset/mock-merchant-android.html");
        
        // 2. For Android Emulator, try this:
         webView.loadUrl("http://10.0.2.2:8080");
        
        // 3. For Physical Device, use your computer's IP:
        // webView.loadUrl("http://10.0.0.29:8080");
        
        // 4. Standard localhost (might not work on all devices):
        // webView.loadUrl("http://127.0.0.1:8080");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack(); else finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            if (webView.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) webView.getParent()).removeView(webView);
            }
            webView.destroy();
        }
        super.onDestroy();
    }
}
