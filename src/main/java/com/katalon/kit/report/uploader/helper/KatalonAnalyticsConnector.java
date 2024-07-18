package com.katalon.kit.report.uploader.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.kit.report.uploader.model.UploadInfo;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class KatalonAnalyticsConnector {

    private static final long MAX_WAIT_INTERVAL = 10000;
    private static final long MAX_RETRIES = 10;
    private static final Logger LOG = LogHelper.getLogger();
    private static final String KATALON_TEST_REPORTS_URI = "/api/v1/katalon/test-reports";
    private static final String KATALON_RECORDER_TEST_REPORTS_URI = "/api/v1/katalon-recorder/test-reports";
    private static final String KATALON_JUNIT_TEST_REPORTS_URI = "/api/v1/junit/test-reports";
    private static final String UPLOAD_URL_URI = "/api/v1/files/upload-url";
    private static final String TOKEN_URI = "/oauth/token";

    private final ExceptionHelper exceptionHelper;
    private final HttpHelper httpHelper;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public KatalonAnalyticsConnector(
            ExceptionHelper exceptionHelper,
            HttpHelper httpHelper,
            ApplicationProperties applicationProperties,
            ObjectMapper objectMapper) {
        this.exceptionHelper = exceptionHelper;
        this.httpHelper = httpHelper;
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    public void uploadFileInfo(
            long projectId,
            String batch,
            String folderName,
            String fileName,
            String uploadedPath,
            boolean isEnd,
            String token,
            boolean pushToXray) {

        final String url;
        switch (applicationProperties.getReportType()) {
            case katalon:
                url = applicationProperties.getServerApiUrl() + KATALON_TEST_REPORTS_URI;
                break;
            case katalon_recorder:
                url = applicationProperties.getServerApiUrl() + KATALON_RECORDER_TEST_REPORTS_URI;
                break;
            case junit:
                url = applicationProperties.getServerApiUrl() + KATALON_JUNIT_TEST_REPORTS_URI;
                break;
            default:
                throw new IllegalStateException();
        }

        try {
            final URIBuilder uriBuilder = new URIBuilder(url);

            uriBuilder.setParameter("projectId", String.valueOf(projectId));
            uriBuilder.setParameter("batch", batch);
            uriBuilder.setParameter("folderPath", folderName);
            uriBuilder.setParameter("isEnd", String.valueOf(isEnd));
            uriBuilder.setParameter("fileName", fileName);
            uriBuilder.setParameter("uploadedPath", uploadedPath);
            uriBuilder.setParameter("pushToXray", String.valueOf(pushToXray));

            HttpPost httpPost = new HttpPost(uriBuilder.build());
            httpHelper.sendRequest(
                httpPost,
                token,
                null,
                null,
                null,
                null,
                null
            );
        } catch (Exception e) {
            LOG.error("Cannot send data to server: {}", url, e);
            exceptionHelper.wrap(e);
        }
    }

    public UploadInfo getUploadInfo(String token, long projectId) {
        final String url = applicationProperties.getServerApiUrl() + UPLOAD_URL_URI;
        try {
            final URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameter("projectId", String.valueOf(projectId));

            final HttpGet httpGet = new HttpGet(uriBuilder.build());
            final HttpResponse httpResponse = httpHelper.sendRequest(
                httpGet,
                token,
                null,
                null,
                null,
                null,
                null
            );

            InputStream content = httpResponse.getEntity().getContent();
            return objectMapper.readValue(content, UploadInfo.class);
        } catch (Exception e) {
            LOG.error("Cannot send data to server: {}", url, e);
            return exceptionHelper.wrap(e);
        }
    }

    /*
     * Returns the next wait interval, in milliseconds, using an exponential
     * backoff algorithm.
     */
    public static long getWaitTimeExp(int retryCount) {
        if (0 == retryCount) {
            return 0;
        }

        return ((long) Math.pow(2, retryCount) * 100L);
    }

    public void uploadFileWithRetry(String url, File file) {
        int retries = 0;
        boolean retry = false;

        do {
            try {
                // Get the result of the asynchronous operation.
                int statusCode = uploadFile(url, file);

                if (needToRetry(statusCode)) {
                    retry = true;
                    long waitTime = Math.min(getWaitTimeExp(retries), MAX_WAIT_INTERVAL);
                    LOG.info("Wait {}ms until retry", waitTime);

                    // Wait to retry
                    Thread.sleep(waitTime);
                }
            } catch (Exception e) {
                LOG.error("Error when uploading files", e);
            }
        } while (retry && (retries++ < MAX_RETRIES));

    }

    private boolean needToRetry(int statusCode) {
        // server error
        return 500 <= statusCode && statusCode <= 599;
    }

    /*
     * Return status code of upload file request.
     * If failed to send request, return 0.
     */
    public int uploadFile(String url, File file) {
        try (final InputStream content = Files.newInputStream(file.toPath())) {
            final HttpPut httpPut = new HttpPut(url);
            final HttpResponse httpResponse = httpHelper.sendRequest(
                httpPut,
                null,
                null,
                null,
                content,
                file.length(),
                null
            );

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            LOG.info(statusCode + " " + url);

            return statusCode;
        } catch (Exception e) {
            LOG.error("Cannot send data to server: {}", url, e);
            return exceptionHelper.wrap(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public String requestToken(String email, String password) {
        try {
            final String url = applicationProperties.getServerApiUrl() + TOKEN_URI;
            final URIBuilder uriBuilder = new URIBuilder(url);

            final List<NameValuePair> pairs = Arrays.asList(
                new BasicNameValuePair("username", email),
                new BasicNameValuePair("password", password),
                new BasicNameValuePair("grant_type", applicationProperties.getServerApiOAuth2GrantType())
            );

            final HttpPost httpPost = new HttpPost(uriBuilder.build());

            final String serverApiOAuth2ClientId = applicationProperties.getServerApiOAuth2ClientId();
            final String serverApiOAuth2ClientSecret = applicationProperties.getServerApiOAuth2ClientSecret();
            final String clientCredentials = serverApiOAuth2ClientId + ":" + serverApiOAuth2ClientSecret;
            httpPost.setHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(clientCredentials.getBytes())
            );

            HttpResponse httpResponse = httpHelper.sendRequest(
                httpPost,
                null,
                serverApiOAuth2ClientId,
                serverApiOAuth2ClientSecret,
                null,
                null,
                pairs
            );

            try (final InputStream content = httpResponse.getEntity().getContent()) {
                final Map<String, Object> map = objectMapper.readValue(content, Map.class);
                return (String) map.get("access_token");
            }
        } catch (Exception e) {
            LOG.error("Cannot get access_token from server by your credentials", e);
            return exceptionHelper.wrap(e);
        }
    }
}
