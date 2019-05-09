package io.github.sinri.passover.gateway.config;

import io.github.sinri.passover.gateway.AbstractRequestFilter;
import io.github.sinri.passover.gateway.GatewayRequest;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;

public class RequestFilterFactory {
    public String className;
    public Map<String, Object> configMap;

    public RequestFilterFactory(Map<String, Object> castedMap) {
        className = (String) castedMap.get("class");
        configMap = (Map<String, Object>) castedMap.getOrDefault("config", null);
    }

    public RequestFilterFactory(String className, Map<String, Object> configMap) {
        this.className = className;
        this.configMap = configMap;
    }

    /**
     * @param request the gateway request
     * @return the filter instance
     */
    public AbstractRequestFilter buildInstanceWithGatewayRequest(GatewayRequest request) {
        try {
            Class<? extends AbstractRequestFilter> filterClass = Class.forName(className).asSubclass(AbstractRequestFilter.class);

            LoggerFactory.getLogger(this.getClass()).info("Filter " + filterClass + " 到达门口");

            AbstractRequestFilter filter = filterClass.getDeclaredConstructor(GatewayRequest.class).newInstance(request);
            filter.loadConfig(configMap);

            return filter;
        } catch (ClassNotFoundException e) {
            LoggerFactory.getLogger(this.getClass()).error("Route加载Filter类 " + className + " 不正确，此Filter将被无视", e);
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("发生了未能预期的故障！" + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}