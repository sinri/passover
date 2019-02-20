package com.sinri.passover.VertxHttp;

public class PassoverRoute {
    private String host;
    private int port;
    private boolean useSSL;
    private String uri;
    private boolean shouldBeFiltered;
    private boolean shouldBeAbandoned;
    private boolean shouldFilterWithBody;

    public boolean isShouldBeFiltered() {
        return shouldBeFiltered;
    }

    public PassoverRoute setShouldBeFiltered(boolean shouldBeFiltered) {
        this.shouldBeFiltered = shouldBeFiltered;
        return this;
    }

    public boolean isShouldBeAbandoned() {
        return shouldBeAbandoned;
    }

    public PassoverRoute setShouldBeAbandoned(boolean shouldBeAbandoned) {
        this.shouldBeAbandoned = shouldBeAbandoned;
        return this;
    }

    public boolean isShouldFilterWithBody() {
        return shouldFilterWithBody;
    }

    public PassoverRoute setShouldFilterWithBody(boolean shouldFilterWithBody) {
        this.shouldFilterWithBody = shouldFilterWithBody;
        return this;
    }

    public String getHost() {
        return host;
    }

    public PassoverRoute setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public PassoverRoute setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public PassoverRoute setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public PassoverRoute setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String toString() {
        return "PassoverRoute("
                + "host:" + getHost() + ", port:" + getPort() + ", useSSL:" + isUseSSL() + ", uri:" + getUri()
                + ", shouldBeFiltered:" + shouldBeFiltered
                + ", shouldBeAbandoned:" + shouldBeAbandoned
                + ", shouldFilterWithBody:" + shouldFilterWithBody
                + ")";
    }
}
