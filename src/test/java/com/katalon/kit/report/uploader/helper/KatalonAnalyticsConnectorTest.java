package com.katalon.kit.report.uploader.helper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katalon.kit.report.uploader.model.ReportType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KatalonAnalyticsConnectorTest {

    @Mock
    private ExceptionHelper exceptionHelper;

    @Mock
    private HttpHelper httpHelper;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    private KatalonAnalyticsConnector connector;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        connector = new KatalonAnalyticsConnector();
        ReflectionTestUtils.setField(connector, "exceptionHelper", exceptionHelper);
        ReflectionTestUtils.setField(connector, "httpHelper", httpHelper);
        ReflectionTestUtils.setField(connector, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(connector, "reportType", ReportType.katalon);
        ReflectionTestUtils.setField(connector, "serverApiUrl", "https://example.katalon.io");

        Logger logger = (Logger) LoggerFactory.getLogger(KatalonAnalyticsConnector.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(KatalonAnalyticsConnector.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void logsExecutionUrlWhenUploadResponseContainsWebUrl() throws Exception {
        when(httpHelper.sendRequest(any(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(
                "[{\"webUrl\":\"https://example.katalon.io/team/1/project/2/executions/3\"}]"
                        .getBytes(StandardCharsets.UTF_8)));

        connector.uploadFileInfo(123L, "batch-1", "folder", "report.log", "uploads/report.log", true, "token", false);

        verify(httpHelper).sendRequest(any(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull());
        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertTrue(messages.contains("TestOps execution URL: https://example.katalon.io/team/1/project/2/executions/3"));
    }

    @Test
    void doesNotLogExecutionUrlWhenUploadResponseHasNoWebUrl() throws Exception {
        when(httpHelper.sendRequest(any(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8)));

        connector.uploadFileInfo(123L, "batch-1", "folder", "report.log", "uploads/report.log", true, "token", false);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertTrue(messages.stream().noneMatch(message -> message.contains("TestOps execution URL:")));
    }
}
