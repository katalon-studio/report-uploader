package com.katalon.kit.report.uploader.service;

import com.katalon.kit.report.uploader.helper.*;
import com.katalon.kit.report.uploader.model.UploadInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;

@Service
public class UploadService {
    private static final Logger LOG = LogHelper.getLogger();

    // matches all files not end with ".", ".zip", ".har"
    private static final String LOG_PATTERN = ".*(?i)(?<!\\.|\\.zip|\\.har)$";
    private static final String HAR_PATTERN = ".*(?i)\\.(har)$";

    private final FileHelper fileHelper;
    private final KatalonAnalyticsConnector katalonAnalyticsConnector;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public UploadService(FileHelper fileHelper, KatalonAnalyticsConnector katalonAnalyticsConnector, ApplicationProperties applicationProperties) {
        this.fileHelper = fileHelper;
        this.katalonAnalyticsConnector = katalonAnalyticsConnector;
        this.applicationProperties = applicationProperties;
    }

    public void upload() {
        String token = katalonAnalyticsConnector.requestToken(
                applicationProperties.getEmail(),
                applicationProperties.getPassword()
        );

        if (StringUtils.isNotBlank(token)) {
            perform(token);
        } else {
            LOG.error("Cannot get the access token - please check your credentials and network");
        }
    }

    @SuppressWarnings({"unchecked"})
    private void perform(String token) {
        final String path = applicationProperties.getPath();
        final Long projectId = applicationProperties.getProjectId();
        final boolean pushToXray = applicationProperties.isPushToXray();

        LOG.info("Uploading log files in folder path: {}", path);
        final List<Path> zips = packageHarFiles(path);
        final List<Path> files = fileHelper.scanFiles(path, LOG_PATTERN);
        final String batch = generateBatch();
        files.addAll(zips);

        // no need upload parallel if there is only 1 task or less than 2 files.
        if (applicationProperties.getNumberOfTasks() <= 1 || files.size() <= 2) {
            LOG.info("Uploading files in sequence ...");
            files.forEach(filePath -> {
                boolean isEnd = files.indexOf(filePath) == (files.size() - 1);
                uploadFile(token, projectId, batch, filePath, pushToXray, isEnd);
            });
        } else {
            int numberOfThreads = numberOfThreads(files.size() - 1);
            LOG.info("Uploading files in parallel with number of tasks {} ...", numberOfThreads);

            // use thread executor to manage upload thread, it is very useful when running in Java that support
            // virtual thread.
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            // upload parallel only for n-1 files
            final Iterator<Path> pathIterator = files.iterator();

            // prepare the list of completable future to upload files in parallel
            CompletableFuture<Void>[] uploadFutures = new CompletableFuture[files.size() - 1];
            for (int i = 0; i < uploadFutures.length; i++) {
                uploadFutures[i] = CompletableFuture.runAsync(
                        () -> uploadFile(token, projectId, batch, pathIterator.next(), pushToXray, false),
                        executorService
                );
            }

            try {
                CompletableFuture.allOf(uploadFutures)
                        .thenRun(() -> uploadFile(token, projectId, batch, pathIterator.next(), pushToXray, true))
                        .join();
            } catch (CancellationException | CompletionException ex) {
                LOG.error("Cannot upload files in parallel", ex);
            } finally {
                executorService.shutdown();
            }
        }

        try {
            writeUploadInfo(files, batch);
        } catch (Exception e) {
            LOG.error("Cannot write file", e);
        }
    }

    /**
     * Upload the given file to project and push to Xray system if needed.
     *
     * @param token the given token to authenticate
     * @param projectId the given project Id where we want to upload file to.
     * @param batch the given batch to identify the upload
     * @param filePath the given file path to upload.
     * @param pushToXray the flag that let system know if the user want to push to Xray if needed.
     * @param hasEnded the flag that let system know if the file is the last file in the batch.
     */
    private void uploadFile(String token, Long projectId, String batch, Path filePath, boolean pushToXray, boolean hasEnded) {
        LOG.info("BEGIN uploading the file: {}", filePath.toAbsolutePath());
        try {
            UploadInfo uploadInfo = katalonAnalyticsConnector.getUploadInfo(token, projectId);

            File file = filePath.toFile();
            katalonAnalyticsConnector.uploadFileWithRetry(uploadInfo.getUploadUrl(), file);
            katalonAnalyticsConnector.uploadFileInfo(
                projectId,
                batch,
                filePath.getParent().toString(),
                file.getName(),
                uploadInfo.getPath(),
                hasEnded,
                token,
                pushToXray
            );
        } catch (Exception ex) {
            LOG.error("Cannot upload the file: {}", filePath.toAbsolutePath(), ex);
        } finally {
            LOG.info("END uploading the file: {}", filePath.toAbsolutePath());
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
                zipFile = compress(harFiles, zipFile);
                zips.add(zipFile);
            }
        } catch (Exception ex) {
            LOG.error("Cannot zip Hars file", ex);
        }
        return zips;
    }

    public static Path compress(List<Path> files, Path zipFile) throws IOException {
        try (OutputStream zipFileOutputStream = Files.newOutputStream(zipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(zipFileOutputStream)) {

            for (Path file : files) {
                InputStream fileInputStream = Files.newInputStream(file);
                ZipEntry zipEntry = new ZipEntry(file.toFile().getName());
                zipOutputStream.putNextEntry(zipEntry);

                IOUtils.copy(fileInputStream, zipOutputStream);
                IOUtils.closeQuietly(fileInputStream);
            }

            return zipFile;
        }
    }

    private void writeUploadInfo(List<Path> files, String batch) throws IOException {
        Map<String, Object> logData = new HashMap<>();

        logData.put("batch", batch);
        logData.put("files", files.stream().map(Path::toAbsolutePath).collect(toList()));

        fileHelper.saveUploadInfo(logData, applicationProperties.getUploadInfoFilePath());
    }

    private static String generateBatch() {
        // random a number and UUID to ensure the batch is unique
        SecureRandom random = new SecureRandom();
        random.setSeed(System.currentTimeMillis());

        return String.format("%016x", random.nextLong()) + "-" + UUID.randomUUID();
    }

    /**
     * @return the number of threads to upload files in parallel.
     */
    private int numberOfThreads(int numberOfFiles) {
        return Math.min(numberOfFiles, applicationProperties.getNumberOfTasks());
    }
}
