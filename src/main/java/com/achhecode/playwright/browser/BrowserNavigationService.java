package com.achhecode.playwright.browser;

import com.microsoft.playwright.Page;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class BrowserNavigationService {

    private final TabManager tabManager;
    private final BrowserPageWaiter waiter;
    private final PlaywrightExecutor executor;

    public BrowserNavigationService(TabManager tabManager, BrowserPageWaiter waiter, PlaywrightExecutor executor) {
        this.tabManager = tabManager;
        this.waiter = waiter;
        this.executor = executor;
    }

    /** Navigates the given tab, or the active tab when tabId is null/blank. */
    public TabInfo navigate(String tabId, String url) {
        validate(url);
        return executor.call(() -> {
            BrowserTab tab = resolve(tabId);
            Page page = tab.getPage();
            page.navigate(url);
            waiter.waitUntilDocumentLoaded(page);
            return tabManager.describe(tab);
        });
    }

    public TabInfo reload(String tabId) {
        return executor.call(() -> {
            BrowserTab tab = resolve(tabId);
            tab.getPage().reload();
            return tabManager.describe(tab);
        });
    }

    public TabInfo back(String tabId) {
        return executor.call(() -> {
            BrowserTab tab = resolve(tabId);
            tab.getPage().goBack();
            return tabManager.describe(tab);
        });
    }

    public TabInfo forward(String tabId) {
        return executor.call(() -> {
            BrowserTab tab = resolve(tabId);
            tab.getPage().goForward();
            return tabManager.describe(tab);
        });
    }

    private BrowserTab resolve(String tabId) {
        return (tabId == null || tabId.isBlank()) ? tabManager.getActiveTab() : tabManager.getTab(tabId);
    }

    private void validate(String url) {
        String scheme = URI.create(url).getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new IllegalArgumentException("Only http/https URLs are allowed: " + url);
        }
    }
}
