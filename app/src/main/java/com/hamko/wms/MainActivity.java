package com.hamko.wms;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
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
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
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
                    //.setNegativeButton("No", null)
                    .show();
        } else {
            //Todo: All Webview stuff Goes Here
            webview = findViewById(R.id.webView);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

            webview.getSettings().setAllowFileAccess(true);
            webview.getSettings().setAllowContentAccess(true);
            webview.getSettings().setBlockNetworkImage(false);
            webview.getSettings().setBlockNetworkImage(false);
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
            //webview.setDownloadListener((uri, userAgent, contentDisposition, mimetype, contentLength) -> handleURI(uri));

            webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
                Log.d("TAG", "onCreate: "+url+"-"+"mimeType");
                if(isStoragePermissionGranted()){
                    try {
                        //url = url.replace("blob:","").trim();
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
                    }catch (Exception e){
                        Log.e("TAG", "onCreate: ", e);
                        webview.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url));
                    }
                }else {
                    Toast.makeText(context,"Storage permission need", Toast.LENGTH_LONG).show();
                }
            });
//            webview.setOnLongClickListener(v -> {
//                handleURI(((WebView) v).getHitTestResult().getExtra());
//                return true;
//            });

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
        }else {
            fileChooserCallback.onReceiveValue(null);
        }
        fileChooserCallback = null;
    }

//    private void handleURI(String uri) {
//        if (uri != null) {
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setData(Uri.parse(uri.replaceFirst("^blob:", "")));
//            startActivity(intent);
//        }
//    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("Storage","Permission is granted");
                return true;
            } else {
                Log.v("Storage","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            //Todo: Permission is automatically granted on sdk<23
            Log.v("Storage","Permission is granted");
            return true;
        }
    }

    public static class JavaScriptInterface {
        private Context context;
        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data) throws IOException {
            convertBase64StringToPdfAndStoreIt(base64Data);
        }
        public static String getBase64StringFromBlobUrl(String blobUrl) {
            if(blobUrl.startsWith("blob")){
                return "javascript: var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '"+ blobUrl +"', true);" +
                        "xhr.setRequestHeader('Content-type','application/pdf');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobPdf = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobPdf);" +
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
        private void convertBase64StringToPdfAndStoreIt(String base64PDf) throws IOException {
            final int notificationId = 1;
            String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/YourFileName_" + currentDateTime + "_.pdf");
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:application/pdf;base64,", ""), 0);
            FileOutputStream os;
            os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();

            if (dwldsPath.exists()) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
                intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf"));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                    Notification notification = new Notification.Builder(context,CHANNEL_ID)
                            .setContentText("You have got something new!")
                            .setContentTitle("File downloaded")
                            .setContentIntent(pendingIntent)
                            .setChannelId(CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.sym_action_chat)
                            .build();
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                        notificationManager.notify(notificationId, notification);
                    }

                } else {
                    NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(android.R.drawable.sym_action_chat)
                            //.setContentIntent(pendingIntent)
                            .setContentTitle("MY TITLE")
                            .setContentText("MY TEXT CONTENT");

                    if (notificationManager != null) {
                        notificationManager.notify(notificationId, b.build());
                        Handler h = new Handler();
                        long delayInMilliseconds = 1000;
                        h.postDelayed(new Runnable() {
                            public void run() {
                                notificationManager.cancel(notificationId);
                            }
                        }, delayInMilliseconds);
                    }
                }
            }
            Toast.makeText(context, "PDF FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
        }
    }
}