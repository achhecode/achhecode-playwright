package com.achhecode.playwright.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.achhecode.playwright.capture.BrowserPageCapture;
import com.achhecode.playwright.capture.CaptureStorage;
import com.achhecode.playwright.capture.PageCaptureResult;
import com.achhecode.playwright.exception.PageCaptureException;

@Service
public class PageCaptureService {

    private final BrowserPageCapture browserPageCapture;
    private final CaptureStorage captureStorage;

    public PageCaptureService(BrowserPageCapture browserPageCapture, CaptureStorage captureStorage) {
        this.browserPageCapture = browserPageCapture;
        this.captureStorage = captureStorage;
    }

    public PageCaptureResult captureCurrentPage() {

        BrowserPageCapture.CapturedPage page = browserPageCapture.capture();

        try {
            Path directory = captureStorage.save(page);

            return new PageCaptureResult(directory.toString(), page.url(), page.title());

        } catch (IOException e) {
            throw new PageCaptureException("Failed to save browser page capture.", e);
        }
    }
}
