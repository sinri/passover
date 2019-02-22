package io.github.sinri.passover.sample.Plugin;

import io.github.sinri.passover.gateway.AbstractRequestStatFilter;
import io.github.sinri.passover.gateway.GatewayRequest;

import java.util.Date;
import java.util.HashMap;

public class SampleFlowLimitStatFilter extends AbstractRequestStatFilter {
    static long dictTime;
    static HashMap<String, Integer> dict;

    static {
        dictTime = 0;
        dict = new HashMap<>();
    }

    protected Integer flowLimit = 10;

    public SampleFlowLimitStatFilter(GatewayRequest request) {
        super(request);
    }

    /**
     * 根据这个请求的指定特征计算分类hash
     * 比如 IP，PATH，QUERY 等以及其组合
     *
     * @return 分类HASH
     */
    @Override
    protected String computeCategoryHash() {
        String aa_user_id = (String) request.getFilterShareDataMap().getOrDefault("aa_user_id", "-1");
        String client_ip = request.getRealIpOfIncomingRequest();
        return aa_user_id + "@" + client_ip;
    }

    @Override
    public String getFilterName() {
        return "流量控制示例StatFilter";
    }

    /**
     * 此方法应当被重载以实现需要的过滤机制
     *
     * @return 如果一切正常需要继续转发则返回true，如果需要执行自定义的拒绝回调返回false
     * @throws Exception 如果出现了不可控的异常则扔异常去被abandoned
     */
    @Override
    protected boolean checkPassable() throws Exception {
        long timeInSecond = (new Date()).getTime() / 1000;
        String hash = computeCategoryHash() + "@" + timeInSecond;

        Integer current = safeIncrement(timeInSecond, hash);
        logger.debug("dict updated for hash " + hash + " -> " + current);

        if (current < flowLimit) {
            logger.error("[CC-SAFE] 这一秒 " + hash + " 的访问流量 " + current + " 没有超过了额定值 " + flowLimit + " 准备放行");
            return true;
        } else {
            logger.error("[CC-ALERT] 这一秒 " + hash + " 的访问流量 " + current + " 已经超过了额定值 " + flowLimit + " 准备拒绝");
            return false;
        }
    }

    private static synchronized Integer safeIncrement(long timeInSecond, String hash) {
        if (timeInSecond > dictTime) {
            dict.clear();
            //logger.debug("Dict cleared as out-dated");
            dictTime = timeInSecond;
        }
        Integer previous = dict.put(hash, dict.getOrDefault(hash, 0) + 1);
        Integer current = dict.get(hash);
        //if (previous != null && current != previous + 1) {
        //logger.warn("dict出现了并发问题");
        //}
        return current;
    }
}
