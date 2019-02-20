package com.sinri.passover.VertxHttp;

import io.vertx.core.http.HttpServerRequest;

public class BasePassoverRouter {
    protected String host;
    protected int port;
    protected boolean isSSL;
    protected String uri;
    HttpServerRequest request;

    public BasePassoverRouter(HttpServerRequest request) {
        this.request = request;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isSSL() {
        return isSSL;
    }

    public String getUri() {
        return uri;
    }

    /**
     * Analyze request and compute host, port, isSSL and getUri
     * Override this method if needed.
     *
     * @return if successfully got result
     */
    protected boolean analyzeRoute() {
        computeFallbackRoute();
        return true;
    }

    protected void computeFallbackRoute() {
        String[] hostParts = request.host().split(":");
        host = hostParts[0];
        port = request.isSSL() ? 443 : 80;
        isSSL = request.isSSL();
        uri = request.uri();
    }

    /**
     * First use analyzeRoute,
     * If fails use computeFallbackRoute
     */
    final public void analyze() {
        if (!this.analyzeRoute()) computeFallbackRoute();
    }
}
