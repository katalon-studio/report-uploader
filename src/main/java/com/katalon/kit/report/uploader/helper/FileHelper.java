package com.katalon.kit.report.uploader.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class FileHelper {
    private static final Logger LOG = LogHelper.getLogger();

    private final ObjectMapper objectMapper;
    private final ExceptionHelper exceptionHelper;

    @Autowired
    public FileHelper(ObjectMapper objectMapper, ExceptionHelper exceptionHelper) {
        this.objectMapper = objectMapper;
        this.exceptionHelper = exceptionHelper;
    }

    private Path createPath(String path) {
        final FileSystem fileSystem = FileSystems.getDefault();
        return fileSystem.getPath(path);
    }

    public List<Path> scanFiles(String path, String filePattern) {
        LOG.info("Looking for log files");
        try {
            List<Path> filePaths = new LinkedList<>();
            Path rootPath = createPath(path);
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path visitedFile, BasicFileAttributes attrs) {
                String fileName = visitedFile.toFile().getName();
                boolean matched = Pattern.matches(filePattern, fileName);
                if (matched) {
                    LOG.info("Found file {}", visitedFile.toAbsolutePath());
                    filePaths.add(visitedFile);
                }
                return FileVisitResult.CONTINUE;
                }
            });
            return filePaths;
        } catch (Exception e) {
            LOG.error("Exception when scanning files", e);
            return exceptionHelper.wrap(e);
        }
    }

    public void saveUploadInfo(Map<String, Object> uploadInfo, String filePath) throws IOException {
        if (StringUtils.isNotBlank(filePath)) {
            String infoAsJSONText = objectMapper.writeValueAsString(uploadInfo);
            Files.write(Paths.get(filePath), infoAsJSONText.getBytes());
            LOG.info("Information has been saved to file {}", filePath);
        }
    }
}

