package test.pivotal.pal.tracker.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Map;

public class HttpClient {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    private final OkHttpClient okHttp;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpClient() {
        okHttp = getUnsafeOkHttpClient();
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public Headers headers = new Headers.Builder().build();

    public Response get(String url) {
        return fetch(new Request.Builder().url(url).headers(headers));
    }

    public Response post(String url, Map<String, Object> jsonBody) {
        try {
            Request.Builder reqBuilder = new Request.Builder()
                .url(url).headers(headers)
                .post(RequestBody.create(JSON, objectMapper.writeValueAsString(jsonBody)));

            return fetch(reqBuilder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response post(String url, String formEncoded) {
            Request.Builder reqBuilder = new Request.Builder()
                    .url(url).headers(headers)
                    .post(RequestBody.create(FORM, formEncoded));

            return fetch(reqBuilder);
    }

    public Response put(String url, Map<String, Object> jsonBody) {
        try {
            Request.Builder reqBuilder = new Request.Builder()
                .url(url).headers(headers)
                .put(RequestBody.create(JSON, objectMapper.writeValueAsString(jsonBody)));

            return fetch(reqBuilder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response delete(String url) {
        return fetch(new Request.Builder().delete().url(url).headers(headers));
    }


    private Response fetch(Request.Builder requestBuilder) {
        try {
            Request request = requestBuilder.build();

            okhttp3.Response response = okHttp.newCall(request).execute();
            ResponseBody body = response.body();

            if (body == null) {
                return new Response(response.code(), "");
            }

            return new Response(response.code(), body.string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Response {
        public final int status;
        public final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Response{" +
                "status=" + status +
                ", body='" + body + '\'' +
                '}';
        }
    }
}

