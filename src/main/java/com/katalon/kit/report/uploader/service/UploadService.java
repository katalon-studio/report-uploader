package com.katalon.kit.report.uploader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.kit.report.uploader.helper.*;
import com.katalon.kit.report.uploader.model.UploadInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;

@Service
public class UploadService {
    private static final Logger log = LogHelper.getLogger();

    // matches all files not end with ".", ".zip", ".har"
    private static String LOG_PATTERN = ".*(?i)(?<!\\.|\\.zip|\\.har)$";

    private static String HAR_PATTERN = ".*(?i)\\.(har)$";

    @Autowired
    private FileHelper fileHelper;

    @Autowired
    private KatalonAnalyticsConnector katalonAnalyticsConnector;

    @Autowired
    private KatalonStudioConnector katalonStudioConnector;

    @Autowired
    private ApplicationProperties applicationProperties;

    private String path;

    private String email;

    private String password;

    private Long projectId;

    private String uploadInfoFilePath;

    private Map<String, String> optionalData;

    @PostConstruct
    private void postConstruct() {
        path = applicationProperties.getPath();
        email = applicationProperties.getEmail();
        password = applicationProperties.getPassword();
        projectId = applicationProperties.getProjectId();
        uploadInfoFilePath = applicationProperties.getUploadInfoFilePath();
    }

    public void upload() {
        String token = katalonAnalyticsConnector.requestToken(email, password);
        if (StringUtils.isNotBlank(token)) {
            collectOptionalData();
            generateOptionalDataFile();
            perform(token);
        } else {
            log.error("Cannot get the access token - please check your credentials and network");
        }
    }

    private void collectOptionalData() {
        if (optionalData == null) {
            optionalData = new HashMap<>();
        }
        optionalData.put("buildLabel", applicationProperties.getBuildLabel());
    }

    private void generateOptionalDataFile() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(Paths.get(path, "execution.override_properties").toFile(), optionalData);
        } catch (IOException e) {
            log.error("Failed to handle some parameters.");
        }
    }

    private void perform(String token) {
        log.info("Uploading log files in folder path: {}", path);
        List<Path> zips = packageHarFiles(path);
        List<Path> files = fileHelper.scanFiles(path, LOG_PATTERN);
        String batch = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
        files.addAll(zips);
        for (int i = 0; i < files.size(); i++) {
            Path filePath = files.get(i);
            String folderPath = filePath.getParent().toString();
            boolean isEnd = i == (files.size() - 1);

            log.info("Sending file: {}", filePath.toAbsolutePath());

            File file = filePath.toFile();
            UploadInfo uploadInfo = katalonAnalyticsConnector.getUploadInfo(token, projectId);
            katalonAnalyticsConnector.uploadFileWithRetry(uploadInfo.getUploadUrl(), file);
            katalonAnalyticsConnector.uploadFileInfo(
                    projectId, batch, folderPath, file.getName(), uploadInfo.getPath(), isEnd, token);
        }

        try {
            writeUploadInfo(files, batch);
        } catch (Exception e) {
            log.error("Cannot write file", e);
        }
    }

    private List<Path> packageHarFiles(String path) {
        List<Path> files = fileHelper.scanFiles(path, HAR_PATTERN);

        Map<String, List<Path>> harsMap = files.stream().collect(Collectors.groupingBy(
            filePath -> filePath.getParent().getParent().getParent().toString(),
            LinkedHashMap::new,
            Collectors.toList()));

        List<Path> zips = new ArrayList<>();
        try {
            for (String folderPath : harsMap.keySet()) {
                Path tempPath = Paths.get(folderPath, "katalon-analytics-tmp");
                tempPath.toFile().mkdirs();
                Path zipFile = Paths.get(tempPath.toString(), "hars-" + new Date().getTime() + ".zip");
                zipFile.toFile().createNewFile();
                List<Path> harFiles = harsMap.get(folderPath);
                zipFile= compress(harFiles, zipFile);
                zips.add(zipFile);
            }
        } catch (Exception ex) {
            log.error("Cannot zip hars file", ex);

        }
        return zips;
    }

    public static Path compress(List<Path> files, Path zipfile) throws IOException {
        try (OutputStream zipFileOutputStream = Files.newOutputStream(zipfile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(zipFileOutputStream)) {
            for (Path file : files) {
                InputStream fileInputStream = Files.newInputStream(file);
                ZipEntry zipEntry = new ZipEntry(file.toFile().getName());
                zipOutputStream.putNextEntry(zipEntry);

                IOUtils.copy(fileInputStream, zipOutputStream);
                IOUtils.closeQuietly(fileInputStream);
            }
            return zipfile;
        }
    }

    private void writeUploadInfo(List<Path> files, String batch) throws IOException {
        Map<String, Object> logData = new HashMap<>();
        logData.put("batch", batch);
        logData.put("files", files.stream().map(Path::toAbsolutePath).collect(toList()));
        fileHelper.saveUploadInfo(logData, uploadInfoFilePath);
    }
}
