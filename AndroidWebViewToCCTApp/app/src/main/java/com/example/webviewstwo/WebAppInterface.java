package com.example.webviewstwo;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.util.Log;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * JavaScript interface for WebView communication.
 * This class provides methods that can be called from JavaScript in the web page.
 */
public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private Context context;

    /**
     * Instantiate the interface and set the context
     */
    public WebAppInterface(Context context) {
        this.context = context;
    }

    /**
     * Show a toast from the web page
     * Called from JavaScript: Android.showToast("Hello from JavaScript!")
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Log.d(TAG, "showToast called with: " + toast);
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get device information
     * Called from JavaScript: var info = Android.getDeviceInfo()
     */
    @JavascriptInterface
    public String getDeviceInfo() {
        Log.d(TAG, "getDeviceInfo called");
        return "Android " + android.os.Build.VERSION.RELEASE + 
               " (API " + android.os.Build.VERSION.SDK_INT + ")";
    }

    /**
     * Get app version
     * Called from JavaScript: var version = Android.getAppVersion()
     */
    @JavascriptInterface
    public String getAppVersion() {
        Log.d(TAG, "getAppVersion called");
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version", e);
            return "Unknown";
        }
    }

    /**
     * Log a message from JavaScript to Android logcat
     * Called from JavaScript: Android.logMessage("Debug", "This is a log message")
     */
    @JavascriptInterface
    public void logMessage(String level, String message) {
        switch (level.toLowerCase()) {
            case "debug":
            case "d":
                Log.d(TAG, "JS: " + message);
                break;
            case "info":
            case "i":
                Log.i(TAG, "JS: " + message);
                break;
            case "warning":
            case "warn":
            case "w":
                Log.w(TAG, "JS: " + message);
                break;
            case "error":
            case "e":
                Log.e(TAG, "JS: " + message);
                break;
            default:
                Log.v(TAG, "JS: " + message);
                break;
        }
    }

    /**
     * Example method to demonstrate data exchange
     * Called from JavaScript: Android.sendData(JSON.stringify({key: "value"}))
     */
    @JavascriptInterface
    public void sendData(String jsonData) {
        Log.d(TAG, "sendData called with: " + jsonData);
        // Here you could parse the JSON and handle the data
        // For example, save to preferences, send to server, etc.
        
        // Show confirmation toast
        showToast("Data received: " + jsonData);
    }

    /**
     * Example method to get data from Android to JavaScript
     * Called from JavaScript: var data = Android.getData()
     */
    @JavascriptInterface
    public String getData() {
        Log.d(TAG, "getData called");
        // Return some data (could be from database, preferences, etc.)
        return "{\"status\":\"success\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
    }

    /**
     * Main action handler for webpage communication
     * Called from JavaScript: Android.callAction("ready") or Android.callAction("launchExternalDCF", "{\"url\":\"https://example.com\"}")
     */
    @JavascriptInterface
    public void callAction(String action, String data) {
        Log.d(TAG, "callAction called with action: " + action + ", data: " + data);
        
        switch (action) {
            case "ready":
                handleReadyAction();
                break;
            case "log":
                handleLogAction(data);
                break;
            case "launchExternalDCF":
                handleLaunchExternalDCF(data);
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                showToast("Unknown action: " + action);
                break;
        }
    }

    /**
     * Overloaded method for actions without data
     * Called from JavaScript: Android.callAction("ready")
     */
    @JavascriptInterface
    public void callAction(String action) {
        callAction(action, null);
    }

    /**
     * Handle the 'ready' action - called when webpage is ready
     */
    private void handleReadyAction() {
        Log.i(TAG, "Webpage is ready");
        showToast("Webpage ready!");
        
        // You can add any initialization logic here
        // For example:
        // - Initialize app state
        // - Send configuration to webpage
        // - Start background services
        // - Analytics tracking
    }

    /**
     * Handle the 'log' action - log messages from webpage
     */
    private void handleLogAction(String data) {
        if (data != null && !data.trim().isEmpty()) {
            Log.i(TAG, "Webpage log: " + data);
        } else {
            Log.i(TAG, "Webpage log action called (no data)");
        }
    }

    /**
     * Handle the 'launchExternalDCF' action with data
     * Opens a Chrome Custom Tab with the redirectURL from the data
     * Handles both JSON objects and plain URL strings
     */
    private void handleLaunchExternalDCF(String data) {
        Log.i(TAG, "Launch External DCF requested with data: " + data);
        
        if (data == null || data.trim().isEmpty()) {
            Log.w(TAG, "No data provided for launchExternalDCF action");
            showToast("Error: No data provided for external launch");
            return;
        }

        String redirectURL = null;
        
        try {
            // First, check if data is a valid URL (starts with http:// or https://)
            if (data.startsWith("http://") || data.startsWith("https://")) {
                Log.d(TAG, "Data appears to be a direct URL: " + data);
                redirectURL = data;
            } else {
                // Try to parse as JSON
                JSONObject jsonData = new JSONObject(data);
                
                // Check for different possible URL field names
                if (jsonData.has("redirectURL")) {
                    redirectURL = jsonData.getString("redirectURL");
                } else if (jsonData.has("url")) {
                    redirectURL = jsonData.getString("url");
                } else if (jsonData.has("URL")) {
                    redirectURL = jsonData.getString("URL");
                }
                
                Log.d(TAG, "Extracted URL from JSON: " + redirectURL);
            }
            
            if (redirectURL == null || redirectURL.trim().isEmpty()) {
                Log.w(TAG, "No valid URL found in data: " + data);
                showToast("Error: No URL provided for external launch");
                return;
            }
            
            Log.d(TAG, "Opening Chrome Custom Tab for URL: " + redirectURL);
            launchChromeCustomTab(redirectURL);
            
        } catch (JSONException e) {
            Log.e(TAG, "Data is not valid JSON, treating as direct URL: " + data, e);
            // If JSON parsing fails, treat the entire data as a URL
            if (data.startsWith("http://") || data.startsWith("https://")) {
                launchChromeCustomTab(data);
            } else {
                showToast("Error: Invalid data format for external launch");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling launchExternalDCF", e);
            showToast("Error opening external URL");
        }
    }
    
    /**
     * Launch a Chrome Custom Tab with the specified URL
     */
    private void launchChromeCustomTab(String url) {
        try {
            // Validate and parse the URL
            Uri uri = Uri.parse(url);
            if (uri.getScheme() == null) {
                // Add https if no scheme is provided
                uri = Uri.parse("https://" + url);
            }
            
            // Create CustomTabColorSchemeParams for styling
            CustomTabColorSchemeParams.Builder colorSchemeBuilder = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(context, android.R.color.white))
                    .setNavigationBarColor(ContextCompat.getColor(context, android.R.color.white));
            
            // Build the Custom Tab intent
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(colorSchemeBuilder.build())
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(false)
                    .setStartAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            
            // Create and launch the Custom Tab
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(context, uri);
            
            Log.i(TAG, "Successfully launched Chrome Custom Tab for: " + uri.toString());
            showToast("Opening external URL...");
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching Chrome Custom Tab for URL: " + url, e);
            showToast("Error: Could not open URL");
        }
    }
}