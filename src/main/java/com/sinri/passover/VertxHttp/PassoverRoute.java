package com.sinri.passover.VertxHttp;

import java.util.ArrayList;

public class PassoverRoute {
    private String domain;
    private String serviceHostForProxy;
    private int servicePortForProxy;
    private boolean useHttpsForProxy;
    private boolean useHttpsForVisitor;
    private String uri;
    //private boolean shouldBeFiltered;
    private boolean shouldBeAbandoned;
    private boolean shouldFilterWithBody;
    private ArrayList<Class<? extends AbstractRequestFilter>> filterClasses = new ArrayList<>();

    public String getDomain() {
        return domain;
    }

    public PassoverRoute setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public boolean isUseHttpsForVisitor() {
        return useHttpsForVisitor;
    }

    public PassoverRoute setUseHttpsForVisitor(boolean useHttpsForVisitor) {
        this.useHttpsForVisitor = useHttpsForVisitor;
        return this;
    }

    public ArrayList<Class<? extends AbstractRequestFilter>> getFilterClasses() {
        return filterClasses;
    }

    public PassoverRoute setFilterClasses(ArrayList<Class<? extends AbstractRequestFilter>> filterClasses) {
        this.filterClasses = filterClasses;
        return this;
    }

    public PassoverRoute appendFilterClass(Class<? extends AbstractRequestFilter> filterClass) {
        this.filterClasses.add(filterClass);
        return this;
    }

    public boolean isShouldBeFiltered() {
        return filterClasses != null && !filterClasses.isEmpty();
    }

    public boolean isShouldBeAbandoned() {
        return shouldBeAbandoned;
    }

    public PassoverRoute setShouldBeAbandoned(boolean shouldBeAbandoned) {
        this.shouldBeAbandoned = shouldBeAbandoned;
        return this;
    }

    public boolean isShouldFilterWithBody() {
        return shouldFilterWithBody;
    }

    public PassoverRoute setShouldFilterWithBody(boolean shouldFilterWithBody) {
        this.shouldFilterWithBody = shouldFilterWithBody;
        return this;
    }

    public String getServiceHostForProxy() {
        return serviceHostForProxy;
    }

    public PassoverRoute setServiceHostForProxy(String serviceHostForProxy) {
        this.serviceHostForProxy = serviceHostForProxy;
        return this;
    }

    public int getServicePortForProxy() {
        return servicePortForProxy;
    }

    public PassoverRoute setServicePortForProxy(int servicePortForProxy) {
        this.servicePortForProxy = servicePortForProxy;
        return this;
    }

    public boolean isUseHttpsForProxy() {
        return useHttpsForProxy;
    }

    public PassoverRoute setUseHttpsForProxy(boolean useHttpsForProxy) {
        this.useHttpsForProxy = useHttpsForProxy;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public PassoverRoute setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String restoreIncomeRequestUrl() {
        return (isUseHttpsForVisitor() ? "https" : "http") + "://" + domain + uri;
    }

    @Override
    public String toString() {
        return "PassoverRoute("
                + "serviceHostForProxy:" + getServiceHostForProxy() + ", servicePortForProxy:" + getServicePortForProxy() + ", useHttpsForProxy:" + isUseHttpsForProxy() + ", uri:" + getUri()
                + ", useHttpsForVisitor:" + isUseHttpsForVisitor()
                + ", shouldBeFiltered:" + isShouldBeFiltered()
                + ", shouldBeAbandoned:" + shouldBeAbandoned
                + ", shouldFilterWithBody:" + shouldFilterWithBody
                + ")";
    }
}
