package io.github.sinri.passover.core;

import io.github.sinri.keel.core.properties.KeelOptions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteOptions extends KeelOptions {
    private String routeName;
    private AcceptRequestOptions acceptRequest;
    private String method;
    private MethodRelayOptions relay;

    public String getRouteName() {
        return routeName;
    }

    public RouteOptions setRouteName(String routeName) {
        this.routeName = routeName;
        return this;
    }

    public AcceptRequestOptions getAcceptRequest() {
        return acceptRequest;
    }

    public RouteOptions setAcceptRequest(AcceptRequestOptions acceptRequest) {
        this.acceptRequest = acceptRequest;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public RouteOptions setMethod(String method) {
        this.method = method;
        return this;
    }

    public MethodRelayOptions getRelay() {
        return relay;
    }

    public RouteOptions setRelay(MethodRelayOptions relay) {
        this.relay = relay;
        return this;
    }

    public boolean isApplicableForRequest(String host, String path) {
        if (!getAcceptRequest().getHost().equalsIgnoreCase(host)) {
            return false;
        }
        Pattern pattern = Pattern.compile(getAcceptRequest().getPath());
        Matcher matcher = pattern.matcher(path);
        return matcher.find();
    }

    public static class AcceptRequestOptions extends KeelOptions {
        private String host;
        private String path;

        public String getHost() {
            return host;
        }

        public AcceptRequestOptions setHost(String host) {
            this.host = host;
            return this;
        }

        public String getPath() {
            return path;
        }

        public AcceptRequestOptions setPath(String path) {
            this.path = path;
            return this;
        }
    }

    public static class MethodRelayOptions extends KeelOptions {
        private String host;
        private int port;
        private boolean useHttps;
        private boolean filtersUseBody;
        private List<MethodRelayFilterOptions> filters;

        public String getHost() {
            return host;
        }

        public MethodRelayOptions setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return port;
        }

        public MethodRelayOptions setPort(int port) {
            this.port = port;
            return this;
        }

        public boolean isUseHttps() {
            return useHttps;
        }

        public MethodRelayOptions setUseHttps(boolean useHttps) {
            this.useHttps = useHttps;
            return this;
        }

        public boolean isFiltersUseBody() {
            return filtersUseBody;
        }

        public MethodRelayOptions setFiltersUseBody(boolean filtersUseBody) {
            this.filtersUseBody = filtersUseBody;
            return this;
        }

        public List<MethodRelayFilterOptions> getFilters() {
            return filters;
        }

        public MethodRelayOptions setFilters(List<MethodRelayFilterOptions> filters) {
            this.filters = filters;
            return this;
        }

        public static class MethodRelayFilterOptions extends KeelOptions {
            private String filterName;
            private List<String> filterParams;

            public List<String> getFilterParams() {
                return filterParams;
            }

            public MethodRelayFilterOptions setFilterParams(List<String> filterParams) {
                this.filterParams = filterParams;
                return this;
            }

            public String getFilterName() {
                return filterName;
            }

            public MethodRelayFilterOptions setFilterName(String filterName) {
                this.filterName = filterName;
                return this;
            }
        }
    }
}
