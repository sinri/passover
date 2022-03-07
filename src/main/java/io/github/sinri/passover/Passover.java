package io.github.sinri.passover;

import io.github.sinri.keel.Keel;
import io.github.sinri.keel.core.logger.KeelLogger;
import io.github.sinri.keel.core.properties.KeelOptions;
import io.github.sinri.passover.core.PassoverOptions;
import io.github.sinri.passover.core.RouteOptions;
import io.github.sinri.passover.router.PassoverRoute;
import io.github.sinri.passover.server.PassoverServer;
import io.vertx.core.VertxOptions;

import java.io.IOException;
import java.util.List;

public class Passover {
    private static PassoverOptions passoverOptions;

    public static void main(String[] args) {
        // load passover config in yaml
        try {
            passoverOptions = KeelOptions.loadWithYamlFilePath("config.yml", PassoverOptions.class);
            configPreview();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Keel.loadPropertiesFromFile("keel.properties");
        Keel.initializeVertx(new VertxOptions().setWorkerPoolSize(passoverOptions.getWorkerPoolSize()));

        // start server
        new PassoverServer().serve();

    }

    public static PassoverOptions getPassoverOptions() {
        return passoverOptions;
    }

    protected static void configPreview() {
        KeelLogger logger = Keel.outputLogger("main");
        logger.info("Passover would listen to local port: " + passoverOptions.getLocalListenPort());

        logger.info("Configured Routes, totally " + passoverOptions.getRoutes().size());

        List<RouteOptions> routeOptions = passoverOptions.getRoutes();
        for (var routeOption : routeOptions) {
            logger.info("Route [" + routeOption.getRouteName() + "]");
            logger.info("\twould accept request from " + routeOption.getAcceptRequest().getHost() + ", with path regex: " + routeOption.getAcceptRequest().getPath());
            logger.info("\thandle method: " + routeOption.getMethod());
            if (routeOption.getMethod().equals(PassoverRoute.METHOD_RELAY)) {
                logger.info("\tdestination: " + routeOption.getRelay().getHost() + ":" + routeOption.getRelay().getPort());
                if (routeOption.getRelay().getFilters() != null) {
                    logger.info("\tfilters totally " + routeOption.getRelay().getFilters().size());
                    for (var filter : routeOption.getRelay().getFilters()) {
                        logger.info("\t\tFilter [" + filter.getFilterName() + "] with params: " + filter.getFilterParams());
                    }
                }
            }
        }
    }
}
