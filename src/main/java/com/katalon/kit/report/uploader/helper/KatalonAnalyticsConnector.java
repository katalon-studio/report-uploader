package com.katalon.kit.report.uploader.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.kit.report.uploader.model.ReportType;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class KatalonAnalyticsConnector {

    private static final long MAX_WAIT_INTERVAL = 10000;

    private static final long MAX_RETRIES = 10;

    private static final Logger log = LogHelper.getLogger();

    private static final String KATALON_TEST_REPORTS_URI = "/api/v1/katalon/test-reports";

    private static final String KATALON_RECORDER_TEST_REPORTS_URI = "/api/v1/katalon-recorder/test-reports";

    private static final String KATALON_JUNIT_TEST_REPORTS_URI = "/api/v1/junit/test-reports";

    private static final String UPLOAD_URL_URI = "/api/v1/files/upload-url";

    private static final String TOKEN_URI = "/oauth/token";

    @Autowired
    private ExceptionHelper exceptionHelper;

    @Autowired
    private HttpHelper httpHelper;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private ReportType reportType;

    private String serverApiUrl;

    private String serverApiOAuth2GrantType;

    private String serverApiOAuth2ClientId;

    private String serverApiOAuth2ClientSecret;

    @PostConstruct
    private void postConstruct() {
        reportType = applicationProperties.getReportType();
        serverApiUrl = applicationProperties.getServerApiUrl();
        serverApiOAuth2GrantType = applicationProperties.getServerApiOAuth2GrantType();
        serverApiOAuth2ClientId = applicationProperties.getServerApiOAuth2ClientId();
        serverApiOAuth2ClientSecret = applicationProperties.getServerApiOAuth2ClientSecret();
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

        String url;
        switch (reportType) {
            case katalon:
                url = serverApiUrl + KATALON_TEST_REPORTS_URI;
                break;
            case katalon_recorder:
                url = serverApiUrl + KATALON_RECORDER_TEST_REPORTS_URI;
                break;
            case junit:
                url = serverApiUrl + KATALON_JUNIT_TEST_REPORTS_URI;
                break;
            default:
                throw new IllegalStateException();
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameter("projectId", String.valueOf(projectId));
            uriBuilder.setParameter("batch", batch);
            uriBuilder.setParameter("folderPath", folderName);
            uriBuilder.setParameter("isEnd", String.valueOf(isEnd));
            uriBuilder.setParameter("fileName", fileName);
            uriBuilder.setParameter("uploadedPath", uploadedPath);
            uriBuilder.setParameter("pushToXray", String.valueOf(pushToXray));

            HttpPost httpPost = new HttpPost(uriBuilder.build());
            httpHelper.sendRequest(httpPost, token, null, null, null, null, null);
        } catch (Exception e) {
            log.error("Cannot send data to server: {}", url, e);
            exceptionHelper.wrap(e);
        }
    }

    public UploadInfo getUploadInfo(String token, long projectId) {
        String url = serverApiUrl + UPLOAD_URL_URI;
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameter("projectId", String.valueOf(projectId));

            HttpGet httpGet = new HttpGet(uriBuilder.build());

            HttpResponse httpResponse = httpHelper.sendRequest(
                    httpGet,
                    token,
                    null,
                    null,
                    null,
                    null,
                    null);

            InputStream content = httpResponse.getEntity().getContent();
            UploadInfo uploadInfo = objectMapper.readValue(content, UploadInfo.class);
            return uploadInfo;
        } catch (Exception e) {
            log.error("Cannot send data to server: {}", url, e);
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
        long waitTime = ((long) Math.pow(2, retryCount) * 100L);
        return waitTime;
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
                    log.info("Wait {}ms until retry", waitTime);
                    // Wait to retry
                    Thread.sleep(waitTime);
                }
            } catch (Exception e) {
                log.error("Error when uploading files", e);
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
        try (InputStream content = new FileInputStream(file)) {
            HttpPut httpPut = new HttpPut(url);
            HttpResponse httpResponse = httpHelper.sendRequest(
                    httpPut,
                    null,
                    null,
                    null,
                    content,
                    file.length(),
                    null);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            log.info(statusCode + " " + url);
            return statusCode;
        } catch (Exception e) {
            log.error("Cannot send data to server: {}", url, e);
            return exceptionHelper.wrap(e);
        }
    }

    public String requestToken(String email, String password) {
        try {
            String url = serverApiUrl + TOKEN_URI;
            URIBuilder uriBuilder = new URIBuilder(url);

            List<NameValuePair> pairs = Arrays.asList(
                    new BasicNameValuePair("username", email),
                    new BasicNameValuePair("password", password),
                    new BasicNameValuePair("grant_type", serverApiOAuth2GrantType)
            );

            HttpPost httpPost = new HttpPost(uriBuilder.build());

            String clientCredentials = serverApiOAuth2ClientId + ":" + serverApiOAuth2ClientSecret;
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " +
                    Base64.getEncoder().encodeToString(clientCredentials.getBytes()));

            HttpResponse httpResponse = httpHelper.sendRequest(
                    httpPost,
                    null,
                    serverApiOAuth2ClientId,
                    serverApiOAuth2ClientSecret,
                    null,
                    null,
                    pairs);

            InputStream content = httpResponse.getEntity().getContent();
            Map<String, Object> map = objectMapper.readValue(content, Map.class);
            return (String) map.get("access_token");

        } catch (Exception e) {
            log.error("Cannot get access_token from server by your credentials", e);
            return exceptionHelper.wrap(e);
        }
    }

}
