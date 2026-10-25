package com.achhecode.playwright.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.achhecode.playwright.capture.BrowserPageCapture;
import com.achhecode.playwright.capture.CaptureStorage;
import com.achhecode.playwright.capture.PageCaptureResult;
import com.achhecode.playwright.exception.PageCaptureException;

@Service
public class PageCaptureService {

    private static final Logger log = LoggerFactory.getLogger(PageCaptureService.class);

    private final BrowserPageCapture browserPageCapture;
    private final CaptureStorage captureStorage;

    public PageCaptureService(
            BrowserPageCapture browserPageCapture,
            CaptureStorage captureStorage) {
        this.browserPageCapture = browserPageCapture;
        this.captureStorage = captureStorage;
    }

    public Optional<PageCaptureResult> captureCurrentPage() {

        var capturedPage = browserPageCapture.capture();

        if (capturedPage.isEmpty()) {
            log.info("No browser page is open yet. Skipping page capture.");
            return Optional.empty();
        }

        try {
            Path directory = captureStorage.save(capturedPage.get());

            PageCaptureResult result = new PageCaptureResult(
                    directory.toString(),
                    capturedPage.get().url(),
                    capturedPage.get().title());

            return Optional.of(result);

        } catch (IOException e) {
            throw new PageCaptureException(
                    "Failed to save browser page capture.", e);
        }
    }
}
