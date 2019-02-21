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
                .setDomain(hostParts[0])
                .setServiceHostForProxy(hostParts[0])
                .setServicePortForProxy(request.isSSL() ? 443 : 80)
                .setUseHttpsForProxy(request.isSSL())
                .setUseHttpsForVisitor(request.isSSL() || request.getHeader("X-Forwarded-Proto").equalsIgnoreCase("https"))
                .setUri(request.uri())
                .setShouldBeAbandoned(false)
                .setShouldFilterWithBody(true);
    }
}
