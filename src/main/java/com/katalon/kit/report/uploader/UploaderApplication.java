package com.katalon.kit.report.uploader;

import com.katalon.kit.report.uploader.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UploaderApplication implements CommandLineRunner {

    @Autowired
    private UploadService uploadService;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(UploaderApplication.class, args);
    }

    @Override
    public void run(String... args) {
        uploadService.upload();
    }

}

