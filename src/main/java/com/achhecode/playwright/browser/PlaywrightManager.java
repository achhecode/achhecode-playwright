package com.achhecode.playwright.browser;

import com.achhecode.playwright.config.PlaywrightAttachProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Owns the Playwright driver, the CDP connection and the browser context.
 * Attaches to an externally started Chrome and NEVER closes it. Lazily
 * (re)connects, so the app can start before Chrome does.
 */
@Component
public class PlaywrightManager {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightManager.class);

    private final PlaywrightAttachProperties properties;
    private final PlaywrightExecutor executor;
    private final ChromeProcessManager chrome;

    // Only touched on the playwright worker thread.
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    public PlaywrightManager(PlaywrightAttachProperties properties, PlaywrightExecutor executor,
            ChromeProcessManager chrome) {
        this.properties = properties;
        this.executor = executor;
        this.chrome = chrome;
    }

    @PostConstruct
    void init() {
        if (!properties.isEagerConnect()) {
            return;
        }
        try {
            getContext();
        } catch (RuntimeException e) {
            log.warn("Chrome not reachable at startup ({}). Will retry on first use.", e.getMessage());
        }
    }

    /** Returns a live context, reconnecting to Chrome if necessary. */
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

    /** Forces a fresh CDP connection (e.g. after Chrome was restarted). */
    public BrowserContext reconnect() {
        return executor.call(() -> {
            connect();
            return context;
        });
    }

    /**
     * Hard restart: closes the whole Chrome (all tabs/windows), launches a fresh
     * Chrome with remote debugging, and re-attaches. Requires
     * playwright.attach.chrome-path and a local Chrome.
     */
    public BrowserContext restartBrowser() {
        return executor.call(() -> {
            chrome.requireRestartable(); // validate before closing anything
            ensurePlaywright();
            log.info("Restarting Chrome...");

            if (chrome.isDebugPortOpen()) {
                try {
                    if (!isBrowserConnected()) {
                        connect();
                    }
                    closeChromeViaCdp();
                } catch (RuntimeException e) {
                    log.warn("Graceful close failed: {}", e.getMessage());
                }
                browser = null;
                context = null;
                chrome.waitForPort(false, properties.getStartupTimeoutMs());
            }

            browser = null;
            context = null;
            chrome.launch();
            chrome.waitForPort(true, properties.getStartupTimeoutMs());
            connect();
            return context;
        });
    }

    private void closeChromeViaCdp() {
        try {
            CDPSession session = browser.newBrowserCDPSession();
            session.send("Browser.close");
        } catch (PlaywrightException e) {
            // Expected: the connection drops as Chrome exits.
        }
    }

    // ---------------------------------------------------------------- internals

    private BrowserContext resolveContext() {
        ensurePlaywright();

        if (!isBrowserConnected()) {
            connect();
            return context;
        }
        if (!isContextUsable(context)) {
            context = findUsableContext();
        }
        return context;
    }

    private void connect() {
        ensurePlaywright();
        browser = null;
        context = null; // discard stale refs; never close the external Chrome

        String endpoint = properties.endpoint();
        log.info("Connecting to Chrome over CDP: {}", endpoint);

        try {
            browser = playwright.chromium().connectOverCDP(endpoint,
                    new BrowserType.ConnectOverCDPOptions().setTimeout(properties.getConnectTimeoutMs()));
        } catch (PlaywrightException e) {
            throw new IllegalStateException("Cannot connect to Chrome at " + endpoint
                    + ". Start it with --remote-debugging-port=" + properties.getPort(), e);
        }

        context = findUsableContext();
        log.info("Connected to Chrome ({} tab(s)).", context.pages().size());
    }

    private BrowserContext findUsableContext() {
        for (BrowserContext candidate : browser.contexts()) {
            if (isContextUsable(candidate)) {
                candidate.setDefaultTimeout(properties.getDefaultTimeoutMs());
                candidate.setDefaultNavigationTimeout(properties.getNavigationTimeoutMs());
                return candidate;
            }
        }
        throw new IllegalStateException("Connected to Chrome, but no usable browser context exists.");
    }

    private boolean isBrowserConnected() {
        try {
            return browser != null && browser.isConnected();
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private boolean isContextUsable(BrowserContext candidate) {
        if (candidate == null)
            return false;
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

    @PreDestroy
    void shutdown() {
        log.info("Detaching from Chrome...");
        try {
            executor.run(() -> {
                browser = null;
                context = null;
                if (playwright != null) {
                    try {
                        playwright.close(); // drops the CDP connection only
                    } catch (Exception e) {
                        log.warn("Error closing Playwright: {}", e.getMessage());
                    }
                    playwright = null;
                }
            });
        } catch (RuntimeException e) {
            log.warn("Shutdown interrupted: {}", e.getMessage());
        }
    }
}
