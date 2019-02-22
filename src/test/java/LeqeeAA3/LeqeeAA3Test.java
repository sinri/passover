package LeqeeAA3;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LeqeeAA3Test {
    public static void main1(String[] args) {
        String url = "https://account-auth-v3.leqee.com/api/Login/apiVerifyToken";
        String token = "985AA15507347385c6e55926fe1a";

        String data = "token=" + token;

        Vertx vertx = Vertx.vertx();
        HttpClient httpClient = vertx.createHttpClient();

        RequestOptions requestOptions = new RequestOptions()
                .setHost("account-auth-v3.leqee.com")
                .setPort(443)
                .setSsl(true)
                .setURI("/api/Login/apiVerifyToken"/*+"?"+data*/);
        httpClient.request(HttpMethod.POST, requestOptions, response -> {
            System.out.println("RECEIVED " + response.statusCode() + " : " + response.statusMessage() + " from " + response.request().uri());
            response.bodyHandler(buffer -> {
                System.out.println("Content:\n" + buffer);
            });
            httpClient.close();
            vertx.close();
        })
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .putHeader("Content-Length", "" + data.getBytes().length)
                .write(data)
                .end();
    }

    public static void main(String[] args) throws IOException {
        String urlString = "https://account-auth-v3.leqee.com/api/Login/apiVerifyToken";
        String token = "985AA15507347385c6e55926fe1a";
        String bodyString = "token=" + token + "miss";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(bodyString.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        System.out.println("rsp code:" + conn.getResponseCode() + " message: " + conn.getResponseMessage());

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            System.out.println("rsp:" + sb.toString());
        } else {
            InputStream is = conn.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            System.out.println("err rsp:" + sb.toString());
            //    System.out.println("rsp code:" + conn.getResponseCode());
        }
    }
}
