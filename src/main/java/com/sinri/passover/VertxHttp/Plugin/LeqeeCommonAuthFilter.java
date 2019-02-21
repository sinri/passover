package com.sinri.passover.VertxHttp.Plugin;

import com.sinri.passover.VertxHttp.AbstractRequestFilter;
import com.sinri.passover.VertxHttp.GatewayRequest;

public class LeqeeCommonAuthFilter extends AbstractRequestFilter {
    public LeqeeCommonAuthFilter(GatewayRequest request) {
        super(request);
    }

    @Override
    public String getFilterName() {
        return "LeqeeCommonAuthFilter";
    }

    @Override
    protected boolean shouldThisRequestBeFiltered() {
        return false;
    }

    @Override
    protected void dealFilterDeny() {
        super.dealFilterDeny();
    }
}
