package io.github.sinri.passover.sample.Router;

import io.github.sinri.passover.gateway.BasePassoverRouter;
import io.github.sinri.passover.gateway.PassoverRoute;
import io.github.sinri.passover.sample.Plugin.LeqeeCommonAuthFilter;
import io.github.sinri.passover.sample.Plugin.SampleFlowLimitStatFilter;
import io.vertx.core.http.HttpServerRequest;

public class FirstRouter extends BasePassoverRouter {
    @Override
    public String name() {
        return "FirstRouter";
    }

    @Override
    public PassoverRoute analyze(HttpServerRequest request) throws Exception {
        PassoverRoute route = super.analyze(request);
        if (route.getServiceHostForProxy().equals("testoctet.leqee.com")) {
            route.setServiceHostForProxy("10.29.193.97").setUseHttpsForProxy(false);
        } else if (route.getServiceHostForProxy().equals("tianwenlook.leqee.com")) {
            route.setServiceHostForProxy("10.28.40.105").setUseHttpsForProxy(false);
            route.appendFilterClass(LeqeeCommonAuthFilter.class);
            route.appendFilterClass(SampleFlowLimitStatFilter.class);
        }
        return route;
    }
}
