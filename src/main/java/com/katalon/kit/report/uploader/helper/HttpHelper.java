package com.katalon.kit.report.uploader.helper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

@Component
public class HttpHelper {

    private static final Logger LOG = LogHelper.getLogger();

    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.MAX_VALUE;
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.MAX_VALUE;
    private final ExceptionHelper exceptionHelper;

    @Autowired
    public HttpHelper(ExceptionHelper exceptionHelper) {
        this.exceptionHelper = exceptionHelper;
    }

    private HttpClient getHttpClient() {
        return getHttpClient(DEFAULT_CONNECT_TIMEOUT);
    }

    public HttpResponse sendRequest(
            HttpUriRequest httpRequest,
            String bearerToken,
            String username,
            String password,
            InputStream content,
            Long contentLength,
            List<NameValuePair> pairs) throws IOException {

        HttpClient httpClient = getHttpClient();

        if (bearerToken != null) {
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        if (username != null) {
            String basicToken = username + ":" + password;
            String encodedBasicToken = Base64.getEncoder().encodeToString(basicToken.getBytes());
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedBasicToken);
        }

        HttpEntity entity = null;

        if (content != null) {
            entity = new InputStreamEntity(content, contentLength);
        }

        if (pairs != null) {
            entity = new UrlEncodedFormEntity(pairs);
        }

        if (entity != null) {
            ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
        }

        LOG.debug("Request: {}", httpRequest);
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        LOG.debug("Response: {}", httpResponse);

        return httpResponse;
    }

    private HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslSocketFactory = getSslSocketFactory();
        httpClientBuilder.setSSLSocketFactory(sslSocketFactory)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy());
        return httpClientBuilder;
    }

    private HttpClient getHttpClient(int connectTimeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
                .build();

        return getHttpClientBuilder()
                .setDefaultRequestConfig(config)
                .useSystemProperties()
                .build();
    }

    private SSLConnectionSocketFactory getSslSocketFactory() {
        SSLContext sslContext = getSslContext();
        HostnameVerifier skipHostnameVerifier = new SkipHostnameVerifier();
        return new SSLConnectionSocketFactory(sslContext, skipHostnameVerifier);
    }

    private SSLContext getSslContext() {
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            TrustStrategy trustStrategy = new TrustAllStrategy();

            return sslContextBuilder.loadTrustMaterial(keyStore, trustStrategy)
                    .setProtocol("TLSv1.2")
                    .build();
        } catch (Exception e) {
            LOG.error("Cannot get SSL context", e);
            return exceptionHelper.wrap(e);
        }
    }

    /**
     * Trust all certificates.
     */
    private static class TrustAllStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) {
            return true;
        }
    }

    private static class SkipHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }

    }
}
