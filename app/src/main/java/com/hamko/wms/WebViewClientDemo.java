package com.hamko.wms;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewClientDemo extends WebViewClient {
    @Override
    //Todo: Keep webview in app when clicking links
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if(url.contains(WebURL.dashboardUrl())){
            PreferenceHelper.write("guest",false);
        }
        if(url.contains(WebURL.logoutUrl())){
            PreferenceHelper.write("guest",true);
        }
        view.loadUrl(url);
        return true;
    }
};
