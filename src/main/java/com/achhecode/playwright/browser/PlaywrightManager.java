
package com.achhecode.playwright.browser;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.achhecode.playwright.config.PlaywrightBrowserProperties;
import com.achhecode.playwright.config.PlaywrightBrowserProperties.Mode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

@Component
public class PlaywrightManager {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightManager.class);

    private final PlaywrightBrowserProperties properties;
    private final PlaywrightExecutor executor;

    // Accessed only on the Playwright executor thread.
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private boolean ownsBrowser;

    public PlaywrightManager(
            PlaywrightBrowserProperties properties,
            PlaywrightExecutor executor) {
        this.properties = properties;
        this.executor = executor;
    }

    @PostConstruct
    void init() {
        if (!properties.isEagerConnect()) {
            return;
        }

        try {
            getContext();
        } catch (RuntimeException e) {
            log.warn(
                    "Browser unavailable at startup ({}). "
                            + "Will retry on first use.",
                    e.getMessage());
        }
    }

    /** Returns a usable browser context, creating or reconnecting as needed. */
    public BrowserContext getContext() {
        return executor.call(this::resolveContext);
    }

    public Browser getBrowser() {
        return executor.call(() -> {
            resolveContext();
            return browser;
        });
    }

    public boolean isConnected() {
        return executor.call(this::isBrowserConnected);
    }

    /**
     * Recreates the Playwright connection.
     * ATTACHED mode reconnects without closing the external browser.
     */
    public BrowserContext reconnect() {
        return executor.call(() -> {
            releaseCurrentConnection();
            return resolveContext();
        });
    }

    /**
     * Restarts a browser owned by this library.
     * ATTACHED mode cannot restart the externally owned browser.
     */
    public BrowserContext restartBrowser() {
        return executor.call(() -> {
            if (properties.getMode() == Mode.ATTACHED) {
                throw new IllegalStateException(
                        "Restart is not supported in ATTACHED mode. "
                                + "Restart the external browser separately, "
                                + "then call reconnect().");
            }

            log.info("Restarting managed browser...");

            releaseCurrentConnection();

            return resolveContext();
        });
    }

    private BrowserContext resolveContext() {
        ensurePlaywright();

        if (!isBrowserConnected()) {
            browser = null;
            context = null;
            ownsBrowser = false;

            connect();
            return context;
        }

        if (!isContextUsable(context)) {
            if (properties.getMode() == Mode.ATTACHED) {
                context = findUsableContext();
            } else {
                context = browser.newContext();
                applyTimeouts(context);
            }
        }

        return context;
    }

    private void connect() {
        ensurePlaywright();

        switch (properties.getMode()) {
            case HEADLESS, LAUNCH -> launchBrowser();
            case ATTACHED -> attachToBrowser();
        }

        applyTimeouts(context);

        log.info(
                "Browser ready in {} mode with {} tab(s).",
                properties.getMode(),
                context.pages().size());
    }

    private void launchBrowser() {
        validateLaunchConfiguration();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(
                        properties.getMode() == Mode.HEADLESS
                                || properties.isHeadless());

        String executable = properties.getExecutablePath();

        if (executable != null && !executable.isBlank()) {
            options.setExecutablePath(Path.of(executable));
        }

        options.setArgs(properties.getArgs());

        browser = playwright.chromium().launch(options);
        ownsBrowser = true;

        // A new, isolated context managed by this library.
        context = browser.newContext();
    }

    private void attachToBrowser() {
        String endpoint = properties.endpoint();

        log.info("Attaching to browser over CDP: {}", endpoint);

        browser = playwright.chromium().connectOverCDP(
                endpoint,
                new BrowserType.ConnectOverCDPOptions()
                        .setTimeout(properties.getConnectTimeoutMs()));

        ownsBrowser = false;
        context = findUsableContext();
    }

    private void validateLaunchConfiguration() {
        String executable = properties.getExecutablePath();

        if (executable != null && !executable.isBlank()
                && !Files.isExecutable(Path.of(executable))) {
            throw new IllegalStateException(
                    "Browser executable does not exist or is not executable: "
                            + executable);
        }

        if (properties.getArgs() == null) {
            throw new IllegalStateException(
                    "playwright.browser.args must not be null.");
        }
    }

    private BrowserContext findUsableContext() {
        for (BrowserContext candidate : browser.contexts()) {
            if (isContextUsable(candidate)) {
                applyTimeouts(candidate);
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Connected to browser, but no usable browser context exists.");
    }

    private void applyTimeouts(BrowserContext target) {
        target.setDefaultTimeout(properties.getDefaultTimeoutMs());
        target.setDefaultNavigationTimeout(
                properties.getNavigationTimeoutMs());
    }

    private boolean isBrowserConnected() {
        try {
            return browser != null && browser.isConnected();
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private boolean isContextUsable(BrowserContext candidate) {
        if (candidate == null) {
            return false;
        }

        try {
            candidate.pages();
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private void ensurePlaywright() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
    }

    /**
     * Closes only a browser launched by this library.
     * Closing Playwright drops an attached CDP connection without
     * explicitly closing the externally managed browser.
     */

    private void releaseCurrentConnection() {
        Browser currentBrowser = browser;
        Playwright currentPlaywright = playwright;
        boolean shouldCloseBrowser = ownsBrowser;

        // Clear state first so subsequent calls don't reuse stale references.
        browser = null;
        context = null;
        ownsBrowser = false;
        playwright = null;

        if (shouldCloseBrowser && currentBrowser != null) {
            try {
                if (currentBrowser.isConnected()) {
                    currentBrowser.close();
                }
            } catch (Exception e) {
                log.debug(
                        "Browser was already closed or the Playwright driver "
                                + "is unavailable: {}",
                        e.getMessage());
            }
        }

        if (currentPlaywright != null) {
            try {
                currentPlaywright.close();
            } catch (Exception e) {
                log.debug(
                        "Playwright driver was already closed: {}",
                        e.getMessage());
            }
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down Playwright manager...");

        try {
            executor.run(this::releaseCurrentConnection);
        } catch (RuntimeException e) {
            log.warn("Browser shutdown interrupted: {}", e.getMessage());
        }
    }
}
