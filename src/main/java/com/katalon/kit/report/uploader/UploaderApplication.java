package com.katalon.kit.report.uploader;

import com.katalon.kit.report.uploader.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

@SpringBootApplication
public class UploaderApplication implements CommandLineRunner {

    @Autowired
    private UploadService uploadService;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(UploaderApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (args.length > 0 && "merge-junit".equals(args[0])) {
            if (args.length < 2) {
                System.out.println("Usage: merge-junit <directory-path>");
                System.out.println("Example: merge-junit Reports/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements");
                return;
            }
            String directoryPath = args[1];
            Path mergedFile = uploadService.mergeJUnitXmlFiles(directoryPath);
            if (mergedFile != null) {
                System.out.println("Successfully merged JUnit XML files into: " + mergedFile);
            } else {
                System.out.println("Failed to merge JUnit XML files");
            }
        } else {
            uploadService.upload();
        }
    }

}

