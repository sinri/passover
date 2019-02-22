package io.github.sinri.passover.gateway;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

abstract public class AbstractRequestFilter {

    protected GatewayRequest request;
    private String feedback;
    protected Logger logger;

    public AbstractRequestFilter(GatewayRequest request) {
        this.request = request;
        this.logger = LoggerFactory.getLogger("Filter#" + getFilterName() + "@GR" + request.getRequestId());
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
     * 这是一个FINAL的方法，规定了处理流程
     *
     * @param bodyBuffer If body buffer needed in filtering work
     * @return @return 如果一切正常需要继续转发则返回true，如果要执行Filter定义的拒绝回调并停止转发则返回false
     * @throws Exception 如果出现了不可控的异常则扔异常去被abandoned
     */
    final boolean filter(Buffer bodyBuffer) throws Exception {
        feedback = "Not Checked Yet";
        // 如果出现了不可控情况，直接在这里扔异常
        boolean pass = checkPassable();
        // 如果是可控的情况，调用dealFilterDeny方法并返回false
        if (!pass) {
            dealFilterDeny();
            return false;
        }
        // 如果没问题就交给后面filters和转发器
        return true;
    }

    /**
     * 此方法应当被重载以实现需要的过滤机制
     *
     * @return 如果一切正常需要继续转发则返回true，如果需要执行自定义的拒绝回调返回false
     * @throws Exception 如果出现了不可控的异常则扔异常去被abandoned
     */
    abstract protected boolean checkPassable() throws Exception;

    /**
     * 如果有需要自定义拒绝回调可以重载此方法。重点是要关闭网关请求的连接。
     */
    protected void dealFilterDeny() throws Exception {
        request.abandonIncomingRequest(AbandonReason.AbandonByFilter(new Exception("Filter " + getFilterName() + " 拒绝了访问，使用了默认的Abandon策略")));
    }

}
