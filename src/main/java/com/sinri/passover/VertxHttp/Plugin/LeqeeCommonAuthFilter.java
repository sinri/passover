package com.sinri.passover.VertxHttp.Plugin;

import com.sinri.passover.VertxHttp.AbstractRequestFilter;
import com.sinri.passover.VertxHttp.GatewayRequest;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.json.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LeqeeCommonAuthFilter extends AbstractRequestFilter {
    public LeqeeCommonAuthFilter(GatewayRequest request) {
        super(request);
    }

    @Override
    public String getFilterName() {
        return "LeqeeCommonAuthFilter";
    }

    @Override
    protected boolean checkPassable() {
        DefaultCookie passover_leqee_aa_token = request.getCookieExt().readRequestCookie("passover_leqee_aa_token");
        if (passover_leqee_aa_token == null) {
            // 没有通用cookie，引导到登陆页 -> 返回false让调用dealFilterDeny
            return false;
        }
        // 检查Token是否合法
        return verifyLeqeeAAToken(passover_leqee_aa_token.value());
    }

    @Override
    protected void dealFilterDeny() {
        // 这里要出一个登录页啊啊啊啊
        request.getRequest().response().putHeader("Location", "https://account-auth-v3.leqee.com/frontend/passover-login.html");
        request.getRequest().response().end();
    }

    private boolean verifyLeqeeAAToken(String token) {
        try {
            String urlString = "https://account-auth-v3.leqee.com/api/Login/apiVerifyToken";
            String bodyString = "token=" + token;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(bodyString.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            InputStream is;
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            logger.info("Leqee AA 3 API Response: " + sb.toString());

            JsonObject jsonObject = new JsonObject(sb.toString());
            if (!jsonObject.getString("code", "FAILED").equals("OK")) {
                logger.error("Leqee AA 3 API FAIL MESSAGE: " + jsonObject.getString("data", "Unknown Error"));
                return false;
            }
            JsonObject dataObject = jsonObject.getJsonObject("data");
            String aa_user_id = dataObject.getString("aa_user_id");
            String aa_tp_code = dataObject.getString("aa_tp_code");
            logger.info("Leqee AA 3 API shows this one " + aa_user_id + " came " + (aa_tp_code == null ? "without TP" : "through TP " + aa_tp_code));

            request.getFilterShareDataMap().put("aa_user_id", aa_user_id);
            request.getFilterShareDataMap().put("aa_tp_code", aa_tp_code);

            return true;
        } catch (IOException e) {
            logger.error("verifyLeqeeAAToken failed", e);
            return false;
        }
    }
}
