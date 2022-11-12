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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    String websiteURL = "http://202.53.169.89:1656/439/25/auth/login"; //Sets web url
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
                    vw.goBack();
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
            webview.goBack();
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

    public static class JavaScriptInterface {
        private static String fileMimeType;
        private final Context context;
        public JavaScriptInterface(Context context) {
            this.context = context;
            Toast.makeText(context, "Blob", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data) throws IOException {
            Toast.makeText(context, "Blob Called", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                convertBase64StringToFileAndStoreIt(base64Data);
            }
        }

        public static String getBase64StringFromBlobUrl(String blobUrl,String mimeType) {
            if(blobUrl.startsWith("blob")){
                fileMimeType = mimeType;
                return "javascript: var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '"+ blobUrl +"', true);" +
                        "xhr.setRequestHeader('Content-type','" + mimeType +";charset=UTF-8');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobFile = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobFile);" +
                        "        reader.onloadend = function() {" +
                        "            base64data = reader.result;" +
                        "            Android.getBase64FromBlobData(base64data);" +
                        "        }" +
                        "    }" +
                        "};" +
                        "xhr.send();";
            }
            return "javascript: console.log('It is not a Blob URL');";
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        @SuppressLint("NewApi")
        private void convertBase64StringToFileAndStoreIt(String base64PDf) throws IOException {
            final int notificationId = 1;
            Toast.makeText(context, "Base 64 PDF : Downloading", Toast.LENGTH_SHORT).show();
            String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
            String newTime = currentDateTime.replaceFirst(", ","_").replaceAll(" ","_").replaceAll(":","-");
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(fileMimeType);
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + newTime + "_." + extension);
            String regex = "^data:" + fileMimeType + ";base64,";
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst(regex, ""), 0);
            try {
                FileOutputStream os = new FileOutputStream(dwldsPath);
                os.write(pdfAsBytes);
                os.flush();
                os.close();
            } catch (Exception e) {
                Toast.makeText(context, "FAILED TO DOWNLOAD THE FILE!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            if (dwldsPath.exists()) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
                intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(context,CHANNEL_ID)
                    .setContentText("You have got something new!")
                    .setContentTitle("File downloaded")
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }
            }
            Toast.makeText(context, "FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
        }
    }
}