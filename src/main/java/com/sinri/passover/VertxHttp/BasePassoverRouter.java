package com.sinri.passover.VertxHttp;

import io.vertx.core.http.HttpServerRequest;

public class BasePassoverRouter {

    /**
     * Analyze request and compute host, port, useSSL and getUri, etc
     * Override this method if needed.
     */
    public PassoverRoute analyze(HttpServerRequest request) {
        String[] hostParts = request.host().split(":");
        return new PassoverRoute()
                .setHost(hostParts[0])
                .setPort(request.isSSL() ? 443 : 80)
                .setUseSSL(request.isSSL())
                .setUri(request.uri())
                .setShouldBeAbandoned(false)
                .setShouldBeFiltered(true)
                .setShouldFilterWithBody(true);
    }
}
