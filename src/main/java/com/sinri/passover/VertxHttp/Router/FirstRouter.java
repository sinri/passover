package com.sinri.passover.VertxHttp.Router;

import com.sinri.passover.VertxHttp.BasePassoverRouter;
import com.sinri.passover.VertxHttp.PassoverRoute;
import io.vertx.core.http.HttpServerRequest;

public class FirstRouter extends BasePassoverRouter {
    @Override
    public PassoverRoute analyze(HttpServerRequest request) {
        PassoverRoute route = super.analyze(request);
        if (route.getHost().equals("testoctet.leqee.com")) {
            route.setHost("10.29.193.97").setUseSSL(false);
        } else if (route.getHost().equals("tianwenlook.leqee.com")) {
            route.setHost("10.28.40.105").setUseSSL(false);
        }
        return route;
    }
}
