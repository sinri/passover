package io.github.sinri.passover.sample.Plugin;

import io.github.sinri.passover.gateway.GatewayRequest;
import io.github.sinri.passover.gateway.VertxHttpGateway;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.json.JsonObject;

public class LeqeeCASFilter extends LeqeeCommonAuthFilter {
    private String aaToken;

    protected String getAaTPCode() {
        return VertxHttpGateway.getConfigManager().getPassoverConfig().getCasServiceName();
    }

    public LeqeeCASFilter(GatewayRequest request) {
        super(request);
    }

    @Override
    public String getFilterName() {
        return "LeqeeCASFilter";
    }

    @Override
    protected boolean checkPassable() {
        String redirect_from_leqee_cas = request.getRequest().getParam("redirect_from_leqee_cas");
        String redirect_from_leqee_cas_over = request.getRequest().getParam("redirect_from_leqee_cas_over");
        if (
                (redirect_from_leqee_cas != null && redirect_from_leqee_cas.equalsIgnoreCase("Y"))
                        && (redirect_from_leqee_cas_over == null || !redirect_from_leqee_cas_over.equalsIgnoreCase("Y"))
        ) {
            // verify ticket and fetch aa token
            String ticket = request.getRequest().getParam("ticket");
            if (ticket != null) {
                logger.info("redirect_from_leqee_cas and fetched ticket: " + ticket);
                // if ok, write a cookie called passover_leqee_aa_token and put it as DefaultCookie
                if (!verifyLeqeeCASTicket(ticket)) {
                    logger.error("Ticket Check Failed");
                    return false;
                }

                // request.getRequest().scheme() 因为在SLB后面会强行http，这一点要注意
                request.getRequest().response().setStatusCode(302)
                        .setStatusMessage("Turning to target")
                        // Set-Cookie: oms-xxl-passover_TGT=985AA15572445335cd1aa750625e; expires=Wed, 08-May-2019 15:55:36 GMT; Max-Age=86400; path=/
                        .putHeader("Set-Cookie", "passover_leqee_aa_token=" + aaToken + "; Max-Age=86400; path=/")
                        .putHeader(
                                "Location",
                                request.getRequest().scheme() + "://" + request.getRequest().host()
                                        + request.getRequest().uri() + "&redirect_from_leqee_cas_over=Y"
                        );
                request.getRequest().response().endHandler(event -> {
                    logger.info("姑且结束了报文");
                    request.getRequest().response().close();
                    request.getRequest().connection().close();
                }).end();

                return true;
            }
        }

        DefaultCookie passover_leqee_aa_token = request.getCookieExt().readRequestCookie("passover_leqee_aa_token");
        if (passover_leqee_aa_token == null) {
            // 没有通用cookie，引导到登陆页 -> 返回false让调用dealFilterDeny
            feedback = "没有通用cookie，引导到登陆页";
            return false;
        }

        // 检查Token是否合法
        return verifyLeqeeAAToken(passover_leqee_aa_token.value());
    }

    @Override
    protected void dealFilterDeny() throws Exception {
        // 这里要出一个登录页啊啊啊啊
        logger.info("姑且放一个302去登录页吧");
        request.getRequest().response().setStatusCode(302).setStatusMessage("Please Login First");
        request.getRequest().response().putHeader(
                "Location",
                "https://account-auth-v3.leqee.com/cas/login?service=" + getAaTPCode() + "&tp_login_url=" + request.getRoute().restoreIncomeRequestUrl()
        );
        request.getRequest().response().endHandler(event -> {
            logger.info("姑且结束了报文");
            request.getRequest().response().close();
            request.getRequest().connection().close();
        }).end();
    }

    private boolean verifyLeqeeCASTicket(String ticket) {
        try {
            String urlString = "https://account-auth-v3.leqee.com/cas/validate";
            String bodyString = "service=" + getAaTPCode() + "&ticket=" + ticket;//tp_token neglected

            String response = postToApi(urlString, bodyString);

            JsonObject jsonObject = new JsonObject(response);

            JsonObject serviceResponse = jsonObject.getJsonObject("serviceResponse");
            if (serviceResponse == null) {
                throw new Exception("serviceResponse not found");
            }
            JsonObject authenticationSuccess = serviceResponse.getJsonObject("authenticationSuccess");
            if (authenticationSuccess == null) {
                throw new Exception("authenticationSuccess not found");
            }

            JsonObject attributes = authenticationSuccess.getJsonObject("attributes");
            if (attributes == null) {
                throw new Exception("attributes not found");
            }

            aaToken = attributes.getString("aa_token");
            //String aa_tp_code="oms-xxl-passover";
            String aa_user_id = attributes.getString("user_id");

            feedback = "Leqee AA 3 API shows this one " + aa_user_id + " came through TP " + getAaTPCode() + " and aa token is " + aaToken;
//            logger.info("Leqee AA 3 API shows this one " + aa_user_id + " came " + (aa_tp_code == null ? "without TP" : "through TP " + aa_tp_code));
            request.getFilterShareDataMap().put("aa_user_id", aa_user_id);
            request.getFilterShareDataMap().put("aa_tp_code", getAaTPCode());

            // here should be fetch
            //request.getCookieExt().setResponseCookie(new DefaultCookie("passover_leqee_aa_token",aaToken));

            logger.info("FEEDBACK of verifyLeqeeCASTicket: " + feedback);

            return true;
        } catch (Exception e) {
            feedback = "verifyLeqeeCASTicket failed: " + e.getMessage();
            logger.error("verifyLeqeeCASTicket failed", e);
            return false;
        }
    }


}
