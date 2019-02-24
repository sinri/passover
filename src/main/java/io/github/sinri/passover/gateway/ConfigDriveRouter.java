package io.github.sinri.passover.gateway;

import io.github.sinri.passover.gateway.config.RouterConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class ConfigDriveRouter extends BasePassoverRouter {

    @Override
    public String name() {
        return "ConfigDriveRouter";
    }

    RouterConfig routerConfig;

    public ConfigDriveRouter() {
        // 加载配置文件目录中的router.yml文件
        routerConfig = VertxHttpGateway.getConfigManager().getRouterConfig();
    }

    @Override
    public PassoverRoute analyze(HttpServerRequest request) {
        if (routerConfig == null) return super.analyze(request);

        PassoverRoute route = createBasicRoute(request);

        List<RouterConfig.Rule> rules = routerConfig.rules;
        for (int i = 0; i < rules.size(); i++) {
            RouterConfig.Rule rule = rules.get(i);
            if (
                    (rule.conditions.host == null || route.getDomain().equalsIgnoreCase(rule.conditions.host))
                            && (rule.conditions.path == null || route.getUri().matches(rule.conditions.path))
            ) {
                if (rule.route.domain != null) route.setDomain(rule.route.domain);
                if (rule.route.serviceHostForProxy != null)
                    route.setServiceHostForProxy(rule.route.serviceHostForProxy);
                route.setServicePortForProxy(rule.route.servicePortForProxy);
                route.setUseHttpsForProxy(rule.route.useHttpsForProxy);
                if (rule.route.uri != null) route.setUri(rule.route.uri);
                route.setShouldBeAbandoned(rule.route.shouldBeAbandoned);
                route.setShouldFilterWithBody(rule.route.shouldFilterWithBody);
                if (rule.route.filterClasses != null) {
                    rule.route.filterClasses.forEach(className -> {
                        try {
                            route.appendFilterClass(Class.forName(className).asSubclass(AbstractRequestFilter.class));
                        } catch (ClassNotFoundException e) {
                            LoggerFactory.getLogger(this.getClass()).error("Route加载Filter类 " + className + " 不正确，此Filter将被无视", e);
                        }
                    });
                }

                return route;
            }
        }

        return route;
    }
}
