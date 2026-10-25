package com.achhecode.playwright.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.achhecode.playwright.service.PageCaptureService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/page")
@RequiredArgsConstructor
public class PageController {
    private final PageCaptureService pageCaptureService;

    @PostMapping("/capture")
    public ResponseEntity<String> capture() {
        pageCaptureService.captureCurrentPage();
        return ResponseEntity.ok("Page captured successfully!");
    }
}
