package io.github.sinri.passover.sample.Plugin;

import io.github.sinri.passover.gateway.GatewayRequest;

public class LeqeeCASFilterForOmsXXL extends LeqeeCASFilter {
    public LeqeeCASFilterForOmsXXL(GatewayRequest request) {
        super(request);
    }

    @Override
    protected String getAaTPCode() {
        return "oms-xxl-passover";
    }
}
