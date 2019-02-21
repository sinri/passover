package com.sinri.passover.VertxHttp.WebExt;

import io.netty.handler.codec.http.cookie.DefaultCookie;

import java.util.HashMap;
import java.util.Map;

public class CookieExt {
    private Map<String, DefaultCookie> parsedCookieMap;
    private Map<String, DefaultCookie> respondCookieMap;

    public CookieExt(String cookieHeader) {
        parsedCookieMap = new HashMap<>();
        respondCookieMap = new HashMap<>();

        String[] cookieStringParts = cookieHeader.split(";\\s*");
        for (String cookieStringPart : cookieStringParts) {
            String[] pair = cookieStringPart.split("=", 2);
            //if(pair.length<2)continue;// maybe no need
            parsedCookieMap.put(pair[0], new DefaultCookie(pair[0], pair[1]));
        }
    }

    public Map<String, DefaultCookie> getParsedCookieMap() {
        return parsedCookieMap;
    }

    public Map<String, DefaultCookie> getRespondCookieMap() {
        return respondCookieMap;
    }

    public DefaultCookie readRequestCookie(String name) {
        return parsedCookieMap.get(name);
    }

    public CookieExt setResponseCookie(String name, DefaultCookie value) {
        respondCookieMap.put(name, value);
        return this;
    }


}
