package com.sinri.passover.VertxHttp;

import io.vertx.core.buffer.Buffer;

abstract public class AbstractRequestFilter {

    GatewayRequest request;
    private String feedback;

    public AbstractRequestFilter(GatewayRequest request) {
        this.request = request;
    }

    public String getFeedback() {
        return feedback;
    }

    abstract public String getFilterName();

    /**
     * Let the request be filtered.
     * The filters would be chained.
     * The feedback should be updated.
     * If the result is false, the request would be thrown away.
     * If you need to share some data among filters,
     * Use request.getFilterShareDataMap()
     *
     * @param bodyBuffer If body buffer needed in filtering work
     * @return If the request is validated.
     */
    final boolean filter(Buffer bodyBuffer) {
        try {
            feedback = "Not Checked Yet";
            return shouldThisRequestBeFiltered();
        } catch (Exception e) {
            request.getLogger().error("不能通过 " + getFilterName() + " 的检查。" + feedback, e);
            return false;
        }
    }

    abstract protected boolean shouldThisRequestBeFiltered();
}
