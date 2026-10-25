package com.achhecode.playwright.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.achhecode.playwright.config.PlaywrightAttachProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Starts a local Chrome with remote debugging and probes its debug port. */
@Component
public class ChromeProcessManager {

    private static final Logger log = LoggerFactory.getLogger(ChromeProcessManager.class);

    private final PlaywrightAttachProperties props;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    public ChromeProcessManager(PlaywrightAttachProperties props) {
        this.props = props;
    }

    /** Fails fast, BEFORE anything is closed, if a restart cannot succeed. */
    public void requireRestartable() {
        String host = props.getHost();
        if (!(host.equals("127.0.0.1") || host.equals("localhost"))) {
            throw new IllegalStateException("Restart is only supported for a local Chrome (host=" + host + ").");
        }
        String path = props.getChromePath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("playwright.attach.chrome-path is not configured.");
        }
        if (!Files.isExecutable(Path.of(path))) {
            throw new IllegalStateException("Chrome is not executable at: " + path);
        }
    }

    public boolean isDebugPortOpen() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(props.endpoint() + "/json/version"))
                    .timeout(Duration.ofSeconds(1)).GET().build();
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void launch() {
        List<String> command = new ArrayList<>();
        command.add(props.getChromePath());
        command.add("--remote-debugging-port=" + props.getPort());
        if (props.getUserDataDir() != null && !props.getUserDataDir().isBlank()) {
            command.add("--user-data-dir=" + props.getUserDataDir());
        }
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.addAll(props.getChromeArgs());
        command.add("about:blank");

        log.info("Launching Chrome: {}", command);
        try {
            new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to launch Chrome: " + e.getMessage(), e);
        }
    }

    /** Polls until the debug port is open (open=true) or closed (open=false). */
    public void waitForPort(boolean open, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isDebugPortOpen() == open) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Chrome.", e);
            }
        }
        throw new IllegalStateException(
                "Chrome debug port did not become " + (open ? "available" : "free") + " within " + timeoutMs + " ms.");
    }
}
