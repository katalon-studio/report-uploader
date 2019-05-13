package com.katalon.kit.report.uploader.helper;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

@Component
public class KatalonStudioConnector {

    public String getFolderPath(Path filePath) {
        String folderPath;
        try {
            if (FilenameUtils.getExtension(filePath.getFileName().toString()).endsWith("zip")) {
                folderPath = filePath.getParent().getParent().getParent().toFile().getName() + File.separator +
                    filePath.getParent().getParent().toFile().getName()
                    + File.separator
                    + filePath.getParent().toFile().getName();
            } else {
                folderPath = filePath.getParent().getParent().toFile().getName()
                    + File.separator
                    + filePath.getParent().toFile().getName();
            }
        } catch (Exception ex) {
            folderPath = filePath.getParent().toFile().getName();
        }
        return folderPath;
    }
}
