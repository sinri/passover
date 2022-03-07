package io.github.sinri.passover.server;

import io.github.sinri.keel.Keel;
import io.github.sinri.keel.core.logger.KeelLogger;
import io.github.sinri.passover.Passover;
import io.github.sinri.passover.router.PassoverRoute;

public class PassoverServer {
    KeelLogger logger;

    public PassoverServer() {
        logger = Keel.logger("Passover");
    }

    public void serve() {
        Keel.getVertx().createHttpServer()
                .requestHandler(httpServerRequest -> {
                    PassoverRoute mappedRoute = PassoverRoute.findMappedRoute(httpServerRequest);
                    if (mappedRoute == null) {
                        logger.fatal("No matched route, a 403 response to go.");
                        httpServerRequest.response().setStatusCode(403).end();
                        return;
                    }
                    logger.info("Turn to route " + mappedRoute.getRouteName() + " to handle.");
                    mappedRoute.handleRequest(httpServerRequest);
                })
                .exceptionHandler(throwable -> {
                    logger.exception("passoverHttpServer exception", throwable);
                })
                .listen(Passover.getPassoverOptions().getLocalListenPort());
    }

}
