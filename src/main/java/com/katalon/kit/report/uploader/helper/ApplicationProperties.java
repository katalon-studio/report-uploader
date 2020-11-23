package com.katalon.kit.report.uploader.helper;

import com.katalon.kit.report.uploader.model.ReportType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {

    // should not be changed

    @Value("${kit.server.api.oauth2.client_id}")
    private String serverApiOAuth2ClientId;

    @Value("${kit.server.api.oauth2.client_secret}")
    private String serverApiOAuth2ClientSecret;

    @Value("${kit.server.api.oauth2.grant_type}")
    private String serverApiOAuth2GrantType;

    @Value("${server}")
    private String serverApiUrl;

    @Value("${type}")
    private ReportType reportType;

    @Value("${uploadInfoOutPath}")
    private String uploadInfoFilePath;

    // must be provided

    @Value("${email}")
    private String email;

    @Value("${password}")
    private String password;

    @Value("${path}")
    private String path;

    @Value("${projectId}")
    private Long projectId;

    @Value("${report-path}")
    private String reportPath;

    @Value("${project-id}")
    private Long testopsProjectId;

    @Value("${buildLabel}")
    private String buildLabel;

    public String getServerApiOAuth2ClientId() {
        return serverApiOAuth2ClientId;
    }

    public String getServerApiOAuth2ClientSecret() {
        return serverApiOAuth2ClientSecret;
    }

    public String getServerApiOAuth2GrantType() {
        return serverApiOAuth2GrantType;
    }

    public String getServerApiUrl() {
        if (StringUtils.isBlank(serverApiUrl)) {
            return "https://analytics.katalon.com";
        } else {
            return serverApiUrl;
        }
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getUploadInfoFilePath() {
        return uploadInfoFilePath;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPath() {
        return StringUtils.isBlank(reportPath) ? path : reportPath;
    }

    public Long getProjectId() {
        return testopsProjectId == null ? projectId : testopsProjectId;
    }

    public String getBuildLabel() {
        return buildLabel;
    }

}
