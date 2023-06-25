package com.hamko.wms;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    String websiteURL = "http://202.53.169.89:1656/439/25/dealerapp";
    private WebView webview;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ValueCallback<Uri[]> fileChooserCallback;
    Context context;
    @SuppressLint("AddJavascriptInterface")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        PreferenceHelper.init(context);
        //Todo: returns true if internet available
        if (!CheckNetwork.isInternetAvailable(this)) {
            setContentView(R.layout.activity_main);
            //Todo: alert the person knowing they are about to close
            new AlertDialog.Builder(this)
                    .setTitle("No internet connection available")
                    .setMessage("Please Check you're Mobile data or Wifi network.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } else {
            //Todo: All Webview stuff Goes Here
            webview = findViewById(R.id.webView);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

            webview.getSettings().setAllowFileAccess(true);
            webview.getSettings().setAllowContentAccess(true);
            webview.getSettings().setAllowFileAccessFromFileURLs(true);
            webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
            webview.getSettings().setBlockNetworkImage(false);
            webview.getSettings().setDisplayZoomControls(false);
            webview.getSettings().setDomStorageEnabled(true);
            webview.getSettings().setDatabaseEnabled(true);
            webview.getSettings().setGeolocationEnabled(true);
            webview.getSettings().setLoadWithOverviewMode(true);
            webview.getSettings().setLoadsImagesAutomatically(true);
            webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
            webview.getSettings().setPluginState(WebSettings.PluginState.ON);
            webview.getSettings().setSupportMultipleWindows(true);
            webview.getSettings().setUseWideViewPort(true);
            webview.getSettings().setDefaultTextEncodingName("utf-8");
            webview.getSettings().setUseWideViewPort(true);
            webview.getSettings().setAppCachePath(MainActivity.this.getApplicationContext().getCacheDir().getAbsolutePath());
            webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this), "Android");

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            webview.setWebViewClient(new WebViewClient() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public boolean shouldOverrideUrlLoading(WebView vw, WebResourceRequest request) {
                    if (request.getUrl().toString().contains(websiteURL)) {
                        // String webUrl = webview.getUrl();
                        if(request.getUrl().toString().contains("http://202.53.169.89:1656/439/25/auth/logout")){

                        }
                        Log.d("webUrl-", request.getUrl().toString());
                        vw.loadUrl(request.getUrl().toString());
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                        vw.getContext().startActivity(intent);
                    }
                    return true;
                }
            });
            webview.setWebChromeClient(new WebChromeClient() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    request.grant(request.getResources());
                }

                @Override
                public boolean onShowFileChooser(WebView vw, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    if (fileChooserCallback != null) {
                        fileChooserCallback.onReceiveValue(null);
                        fileChooserCallback = null;
                    }
                    fileChooserCallback = filePathCallback;

                    Intent selectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    selectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    selectionIntent.setType("*/*");

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, selectionIntent);
                    startActivityForResult(chooserIntent, 0);

                    return true;
                }
            });
            webview.setOnKeyListener((v, keyCode, event) ->
            {
                WebView vw = (WebView) v;
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && vw.canGoBack()) {

                    if (PreferenceHelper.read("guest",false)){
                        new AlertDialog.Builder(this)
                            .setTitle("EXIT")
                            .setMessage("Are you sure. You want to close this app?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                    }else {
                        vw.goBack();
                    }
                    return true;
                }
                return false;
            });

            webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
                if(isStoragePermissionGranted()){
                    if(url.startsWith("blob:")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            webview.evaluateJavascript(JavaScriptInterface.getBase64StringFromBlobUrl(url,mimeType), null);
                        }
                    }else {
                        //url = url.trim();
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                        request.setMimeType(mimeType);
                        //------------------------COOKIE!!------------------------
                        String cookies = CookieManager.getInstance().getCookie(url);
                        request.addRequestHeader("cookie", cookies);
                        //------------------------COOKIE!!------------------------
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Downloading file...");
                        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                        DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(context,"Downloading", Toast.LENGTH_LONG).show();
                    }
                }else {
                    Toast.makeText(context,"Storage permission need", Toast.LENGTH_LONG).show();
                }
            });
            webview.loadUrl(websiteURL);
            webview.setWebViewClient(new WebViewClientDemo());
        }

        //Todo: Swipe refresh functionality
        mySwipeRefreshLayout = this.findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        webview.reload();
                    }
                }
        );
    }


    private class WebViewClientDemo extends WebViewClient {
        @Override
        //Todo: Keep webview in app when clicking links
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(url.contains("http://202.53.169.89:1656/439/25/dealerpage")){
                PreferenceHelper.write("guest",false);
            }
            if(url.contains("http://202.53.169.89:1656/439/25/auth/logout")){
                PreferenceHelper.write("guest",true);
            }
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mySwipeRefreshLayout.setRefreshing(false);
        }
    };

    //Todo: back button functionality
    @Override
    public void onBackPressed() {
        if (webview.isFocused() && webview.canGoBack()) {
            if (PreferenceHelper.read("guest",false)){
                new AlertDialog.Builder(this)
                    .setTitle("EXIT")
                    .setMessage("Are you sure. You want to close this app?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }else {
                webview.goBack();
            }
        } else {
            //Todo: If the webview cannot go back any further
            new AlertDialog.Builder(this)
                .setTitle("EXIT")
                .setMessage("Are you sure. You want to close this app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (intent != null) {
            fileChooserCallback.onReceiveValue(new Uri[]{Uri.parse(intent.getDataString())});
        } else {
            fileChooserCallback.onReceiveValue(null);
        }
        fileChooserCallback = null;
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            //Todo: Permission is automatically granted on sdk<23
            return true;
        }
    }

}