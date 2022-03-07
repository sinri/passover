package io.github.sinri.passover;

import io.github.sinri.keel.Keel;
import io.github.sinri.keel.core.logger.KeelLogger;
import io.github.sinri.keel.core.properties.KeelOptions;
import io.github.sinri.passover.core.PassoverOptions;
import io.github.sinri.passover.core.RouteOptions;
import io.github.sinri.passover.server.PassoverServer;
import io.vertx.core.VertxOptions;

import java.io.IOException;
import java.util.List;

public class Passover {
    private static PassoverOptions passoverOptions;

    public static void main(String[] args) {
        Keel.loadPropertiesFromFile("keel.properties");
        Keel.initializeVertx(new VertxOptions());

        // load passover config in yaml
        try {
            passoverOptions = KeelOptions.loadWithYamlFilePath("config.yml", PassoverOptions.class);
            testConfigReading();
        } catch (IOException e) {
            e.printStackTrace();
            Keel.getVertx().close();
        }

        // start server
        new PassoverServer().serve();

    }

    public static PassoverOptions getPassoverOptions() {
        return passoverOptions;
    }

    protected static void testConfigReading() {
        KeelLogger logger = Keel.outputLogger("main");
        logger.info("local listen port: " + passoverOptions.getLocalListenPort());

        List<RouteOptions> routeOptions = passoverOptions.getRoutes();
        for (var routeOption : routeOptions) {
            logger.info("route - " + routeOption.getRouteName());
            logger.info("\taccept: " + routeOption.getAcceptRequest().getHost() + " with " + routeOption.getAcceptRequest().getPath());
            logger.info("\tmethod: " + routeOption.getMethod());
            logger.info("\trelay: " + routeOption.getRelay().getHost() + ":" + routeOption.getRelay().getPort());
            logger.info("\tfilters: " + routeOption.getRelay().getFilters().size());
            for (var filter : routeOption.getRelay().getFilters()) {
                logger.info("\t\tfilter " + filter.getFilterName() + " with " + filter.getFilterParams());
            }
        }
    }
}
