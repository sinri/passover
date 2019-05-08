package io.github.sinri.passover.gateway;

import io.vertx.core.http.HttpServerRequest;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasePassoverRouter {

    public String name() {
        return "BasePassoverRouter";
    }

    protected PassoverRoute createBasicRoute(HttpServerRequest request) {
        String[] hostParts = request.host().split(":");
        return new PassoverRoute()
                .setDomain(hostParts[0])
                .setServiceHostForProxy(hostParts[0])
                .setServicePortForProxy(request.isSSL() ? 443 : 80)
                .setUseHttpsForProxy(request.isSSL())
                .setUseHttpsForVisitor(request.isSSL() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))
                .setUri(request.uri())
                .setShouldBeAbandoned(false)
                .setShouldFilterWithBody(true);
    }

    /**
     * Analyze request and compute host, port, useSSL and getUri, etc
     * Override this method if needed.
     * @param request 原始请求
     * @throws Exception Something wrong
     * @return PassoverRoute 新建的route
     */
    public PassoverRoute analyze(HttpServerRequest request) throws Exception {
        return createBasicRoute(request);
    }

    protected PatternMatchingResult parsePathAgainstPattern(HttpServerRequest request, String regex) {
        String path = request.path();

        // 按指定模式在字符串查找
        String pattern = "^" + regex;
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(pattern);
        // 现在创建 matcher 对象
        Matcher m = r.matcher(path);

        PatternMatchingResult patternMatchingResult = new PatternMatchingResult();
        patternMatchingResult.matched = m.find();
        if (patternMatchingResult.matched) {
            patternMatchingResult.parameters = m.toMatchResult();
        }
        return patternMatchingResult;
    }

    class PatternMatchingResult {
        boolean matched = false;
        MatchResult parameters = null;// group[0] is the full path (i think)
    }
}
