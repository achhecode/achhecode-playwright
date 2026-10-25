package com.achhecode.playwright.capture;

import org.springframework.stereotype.Component;

import com.achhecode.playwright.config.PlaywrightAttachProperties;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CaptureStorage {

    private final ObjectMapper objectMapper;
    private final Path capturesDirectory;

    public CaptureStorage(ObjectMapper objectMapper, PlaywrightAttachProperties properties) {
        this.objectMapper = objectMapper;

        this.capturesDirectory = Path.of(properties.getCapture().getDir());
    }

    public Path save(BrowserPageCapture.CapturedPage page) throws IOException {

        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS"));

        Path captureDirectory = capturesDirectory.resolve(timestamp);

        Files.createDirectories(captureDirectory);

        Files.writeString(captureDirectory.resolve("source.html"), page.html(), StandardCharsets.UTF_8);

        Files.write(captureDirectory.resolve("screenshot.png"), page.screenshot());

        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("url", page.url());
        metadata.put("title", page.title());
        metadata.put("capturedAt", OffsetDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(captureDirectory.resolve("metadata.json").toFile(),
                metadata);

        return captureDirectory;
    }
}