package com.sinri.passover.VertxHttp;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.LoggerFactory;

abstract public class AbstractRequestFilter {

    HttpServerRequest request;
    private String feedback;

    public AbstractRequestFilter(HttpServerRequest request) {
        this.request = request;
    }

    public String getFeedback() {
        return feedback;
    }

    /**
     * Let the request be filtered.
     * The filters would be chained.
     * The feedback should be updated.
     * If the result is false, the request would be thrown away.
     *
     * @return If the request is validated.
     */
    final boolean filter() {
        try {
            feedback = "Not Checked Yet";
            return shouldThisRequestBeFiltered();
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Emmm, shouldThisRequestBeFiltered? No.", e);
            return false;
        }
    }

    abstract protected boolean shouldThisRequestBeFiltered();
}
