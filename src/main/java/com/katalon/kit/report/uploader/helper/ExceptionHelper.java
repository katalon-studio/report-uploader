package com.katalon.kit.report.uploader.helper;

import org.springframework.stereotype.Component;

@Component
public class ExceptionHelper {

    public <T> T wrap(Exception e) {
        throw new IllegalStateException(e);
    }
}
