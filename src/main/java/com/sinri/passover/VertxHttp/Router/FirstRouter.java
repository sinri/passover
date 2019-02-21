package com.sinri.passover.VertxHttp.Router;

import com.sinri.passover.VertxHttp.BasePassoverRouter;
import com.sinri.passover.VertxHttp.PassoverRoute;
import com.sinri.passover.VertxHttp.Plugin.LeqeeCommonAuthFilter;
import io.vertx.core.http.HttpServerRequest;

public class FirstRouter extends BasePassoverRouter {
    @Override
    public PassoverRoute analyze(HttpServerRequest request) {
        PassoverRoute route = super.analyze(request);
        if (route.getServiceHostForProxy().equals("testoctet.leqee.com")) {
            route.setServiceHostForProxy("10.29.193.97").setUseHttpsForProxy(false);
        } else if (route.getServiceHostForProxy().equals("tianwenlook.leqee.com")) {
            route.setServiceHostForProxy("10.28.40.105").setUseHttpsForProxy(false);
            route.appendFilterClass(LeqeeCommonAuthFilter.class);
        }
        return route;
    }
}
