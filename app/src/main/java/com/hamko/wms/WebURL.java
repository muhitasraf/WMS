package com.hamko.wms;

public class WebURL {
    private static String baseURL = "https://service.hamkoservice.com/439/25/";

    public static String getBaseUrl(){
        return baseURL;
    }

    public static String homeUrl(){
        return baseURL+"dealerapp";
    }

    public static String logoutUrl(){
        return baseURL+"auth/logout";
    }

    public static String dashboardUrl(){
        return baseURL+"dealerpage";
    }
};
