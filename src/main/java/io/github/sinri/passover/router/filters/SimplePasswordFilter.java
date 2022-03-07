package io.github.sinri.passover.router.filters;

import io.github.sinri.passover.router.PassoverFilter;
import io.vertx.core.http.HttpServerRequest;

public class SimplePasswordFilter extends PassoverFilter {
    private final String tokenName;
    private final String tokenValue;

    public SimplePasswordFilter(String tokenName, String tokenValue) {
        this.tokenName = tokenName;
        this.tokenValue = tokenValue;
    }

    @Override
    public boolean passoverOrNot(HttpServerRequest httpServerRequest) {
        String readFromCookie = httpServerRequest.getCookie(tokenName).getValue();
        getLogger().info("SimplePasswordFilter.passoverOrNot(" + tokenName + "," + tokenValue + ") against " + readFromCookie);
        if (readFromCookie == null) {
            return false;
        }
        return readFromCookie.equals(tokenValue);
    }
}
