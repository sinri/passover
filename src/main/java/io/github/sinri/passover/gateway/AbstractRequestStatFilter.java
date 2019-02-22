package io.github.sinri.passover.gateway;

abstract public class AbstractRequestStatFilter extends AbstractRequestFilter {

    public AbstractRequestStatFilter(GatewayRequest request) {
        super(request);
    }

    /**
     * 根据这个请求的指定特征计算分类hash
     * 比如 IP，PATH，QUERY 等以及其组合
     *
     * @return 分类HASH
     */
    abstract protected String computeCategoryHash();

}
