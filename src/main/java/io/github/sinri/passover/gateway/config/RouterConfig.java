package io.github.sinri.passover.gateway.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouterConfig {
    public String routerName;
    public List<Rule> rules;

    public RouterConfig() {
        routerName = "DefaultRouterName";
        rules = new ArrayList<>();
    }

    public RouterConfig(Map<String, Object> map) {
        routerName = (String) map.getOrDefault("routerName", "DefaultRouterName");
        List<Map<String, Object>> ruleMapList = (List<Map<String, Object>>) map.get("rules");
        if (ruleMapList != null) {
            rules = new ArrayList<>();
            ruleMapList.forEach(ruleMap -> {
                rules.add(new Rule(ruleMap));
            });
        }
    }

    public class Rule {
        public Conditions conditions;
        public Route route;

        public Rule(Map<String, Object> ruleMap) {
            conditions = new Conditions((Map<String, Object>) ruleMap.get("conditions"));
            route = new Route((Map<String, Object>) ruleMap.get("route"));
        }

        public class Conditions {
            public String host;
            public String path;

            public Conditions(Map<String, Object> conditions) {
                host = (String) conditions.get("host");
                path = (String) conditions.get("path");
            }
        }

        public class Route {
            public String domain;
            public String serviceHostForProxy;
            public int servicePortForProxy;
            public boolean useHttpsForProxy;
            //public boolean useHttpsForVisitor;// neglect
            public String uri;
            public boolean shouldBeAbandoned;
            public boolean shouldFilterWithBody;
            public List<String> filterClasses;

            public Route(Map<String, Object> routeMap) {
                domain = (String) routeMap.getOrDefault("domain", null);
                serviceHostForProxy = (String) routeMap.getOrDefault("serviceHostForProxy", null);
                servicePortForProxy = (int) routeMap.getOrDefault("servicePortForProxy", 80);
                useHttpsForProxy = (boolean) routeMap.getOrDefault("useHttpsForProxy", false);
                uri = (String) routeMap.getOrDefault("uri", null);
                shouldBeAbandoned = (boolean) routeMap.getOrDefault("shouldBeAbandoned", false);
                shouldFilterWithBody = (boolean) routeMap.getOrDefault("shouldFilterWithBody", false);
                filterClasses = (List<String>) routeMap.getOrDefault("filterClasses", null);
            }
        }
    }
}
