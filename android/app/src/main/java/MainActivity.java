package com.jasmine.landricsvilla;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Thin Android WebView shell for LandricsVilla.
 *
 * Loads the local-first web frontend from assets/web/index.html by default, or
 * a remote server URL the user configures via the in-app Settings page. No AI
 * provider keys or URLs are hardcoded in this app; the user supplies their own
 * key + base URL through the frontend Settings UI.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "landricsvilla_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String DEFAULT_LOCAL_URL = "file:///android_asset/web/index.html";

    private static final int REQ_PERMS = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // File chooser for uploads initiated from the WebView.
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null) {
                        return;
                    }
                    Uri[] results = null;
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getDataString() != null) {
                            results = new Uri[]{Uri.parse(data.getDataString())};
                        } else if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            results = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                results[i] = data.getClipData().getItemAt(i).getUri();
                            }
                        }
                    }
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep internal loads inside the shell; send external links to the system browser.
                if (url != null
                        && !url.startsWith("file://")
                        && !url.startsWith("http://localhost")
                        && !url.startsWith("http://127.0.0.1")
                        && !url.startsWith("https://localhost")
                        && !url.startsWith("https://127.0.0.1")) {
                    openExternalUrlInternal(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    String[] requested = request.getResources();
                    List<String> granted = new ArrayList<>();
                    for (String r : requested) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)
                                && hasPermission(Manifest.permission.RECORD_AUDIO)) {
                            granted.add(r);
                        } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)
                                && hasPermission(Manifest.permission.CAMERA)) {
                            granted.add(r);
                        }
                    }
                    if (granted.isEmpty()) {
                        request.deny();
                    } else {
                        request.grant(granted.toArray(new String[0]));
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    fileChooserLauncher.launch(Intent.createChooser(intent, "Select file"));
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        // Expose native helpers to the web frontend as window.AndroidBridge.
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        // Back navigation: walk WebView history, then finish the activity.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });

        requestRuntimePermissions();

        webView.loadUrl(getConfiguredUrl());
    }

    private String getConfiguredUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String server = prefs.getString(KEY_SERVER_URL, null);
        if (server != null && !server.isEmpty()) {
            return server;
        }
        return DEFAULT_LOCAL_URL;
    }

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRuntimePermissions() {
        List<String> needed = new ArrayList<>();
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!hasPermission(Manifest.permission.CAMERA)) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMS) {
            return;
        }
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                String p = permissions[i];
                String msg;
                if (Manifest.permission.RECORD_AUDIO.equals(p)) {
                    msg = "Microphone permission denied. Voice input unavailable.";
                } else if (Manifest.permission.CAMERA.equals(p)) {
                    msg = "Camera permission denied. Camera features unavailable.";
                } else {
                    msg = "Storage permission denied. File features may be limited.";
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private String getDeviceIdInternal() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    private void openExternalUrlInternal(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTextInternal(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void configureServerInternal(String url) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (url == null || url.trim().isEmpty()) {
            prefs.edit().remove(KEY_SERVER_URL).apply();
            if (webView != null) {
                webView.loadUrl(DEFAULT_LOCAL_URL);
            }
            Toast.makeText(this, "Switched to bundled offline frontend", Toast.LENGTH_SHORT).show();
        } else {
            prefs.edit().putString(KEY_SERVER_URL, url.trim()).apply();
            if (webView != null) {
                webView.loadUrl(url.trim());
            }
            Toast.makeText(this, "Server configured", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * JavaScript interface exposed to the web frontend as window.AndroidBridge.
     * Methods run on a background WebView thread; UI work is posted to the main thread.
     */
    private class AndroidBridge {
        @android.webkit.JavascriptInterface
        public void shareText(final String text) {
            runOnUiThread(() -> shareTextInternal(text));
        }

        @android.webkit.JavascriptInterface
        public String getDeviceId() {
            return getDeviceIdInternal();
        }

        @android.webkit.JavascriptInterface
        public void openExternalUrl(final String url) {
            runOnUiThread(() -> openExternalUrlInternal(url));
        }

        @android.webkit.JavascriptInterface
        public void configureServer(final String url) {
            runOnUiThread(() -> configureServerInternal(url));
        }
    }
}
