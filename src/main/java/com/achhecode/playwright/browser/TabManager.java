package com.achhecode.playwright.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks tabs with stable ids, discovers tabs opened manually in Chrome, drops
 * closed ones, and remembers the active tab.
 */
@Component
public class TabManager {

    private final PlaywrightManager playwrightManager;
    private final PlaywrightExecutor executor;

    // Guarded by the playwright worker thread.
    private final Map<String, BrowserTab> tabs = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private String activeTabId;

    public TabManager(PlaywrightManager playwrightManager, PlaywrightExecutor executor) {
        this.playwrightManager = playwrightManager;
        this.executor = executor;
    }

    public List<TabInfo> listTabs() {
        return executor.call(() -> sync().stream().map(this::snapshot).toList());
    }

    public BrowserTab getTab(String tabId) {
        return executor.call(() -> {
            sync();
            BrowserTab tab = tabs.get(tabId);
            if (tab == null) {
                throw new NoSuchElementException("Tab not found: " + tabId);
            }
            return tab;
        });
    }

    public BrowserTab getTabByIndex(int index) {
        return executor.call(() -> {
            List<BrowserTab> open = sync();
            if (index < 0 || index >= open.size()) {
                throw new IllegalArgumentException("Invalid tab index " + index + ". Open tabs: " + open.size());
            }
            return open.get(index);
        });
    }

    /** Active tab; falls back to the newest tab, or opens one if none exist. */
    public BrowserTab getActiveTab() {
        return executor.call(() -> {
            List<BrowserTab> open = sync();
            if (activeTabId != null && tabs.containsKey(activeTabId)) {
                return tabs.get(activeTabId);
            }
            BrowserTab tab = open.isEmpty() ? openNewTab() : open.get(open.size() - 1);
            activeTabId = tab.getId();
            return tab;
        });
    }

    public BrowserTab createTab() {
        return executor.call(() -> {
            sync();
            BrowserTab tab = openNewTab();
            activeTabId = tab.getId();
            return tab;
        });
    }

    public BrowserTab activateTab(String tabId) {
        return executor.call(() -> {
            BrowserTab tab = getTab(tabId);
            tab.getPage().bringToFront();
            activeTabId = tab.getId();
            return tab;
        });
    }

    public void closeTab(String tabId) {
        executor.run(() -> {
            BrowserTab tab = getTab(tabId);
            if (!tab.getPage().isClosed()) {
                tab.getPage().close();
            }
            tabs.remove(tabId);
            if (tabId.equals(activeTabId))
                activeTabId = null;
        });
    }

    /**
     * Soft reset: opens one fresh blank tab, closes every other tab, keeps Chrome
     * running. (The new tab is opened first so Chrome never has zero tabs.)
     */
    public BrowserTab resetTabs() {
        return executor.call(() -> {
            List<BrowserTab> existing = sync();
            BrowserTab fresh = openNewTab();
            for (BrowserTab tab : existing) {
                if (!tab.getPage().isClosed()) {
                    tab.getPage().close();
                }
            }
            activeTabId = fresh.getId();
            sync();
            return fresh;
        });
    }

    public TabInfo describe(BrowserTab tab) {
        return executor.call(() -> snapshot(tab));
    }

    // ---------------------------------------------------------------- internals

    private BrowserTab openNewTab() {
        Page page = playwrightManager.getContext().newPage();
        return register(page);
    }

    private BrowserTab register(Page page) {
        BrowserTab tab = new BrowserTab("tab-" + sequence.incrementAndGet(), page);
        tabs.put(tab.getId(), tab);
        return tab;
    }

    /** Reconciles the registry with Chrome; returns open tabs in browser order. */
    private List<BrowserTab> sync() {
        BrowserContext context = playwrightManager.getContext();

        List<Page> pages = context.pages().stream().filter(p -> !p.isClosed()).toList();

        tabs.values().removeIf(t -> t.getPage().isClosed() || !pages.contains(t.getPage()));

        List<BrowserTab> ordered = new ArrayList<>();
        for (Page page : pages) {
            BrowserTab known = null;
            for (BrowserTab t : tabs.values()) {
                if (t.getPage() == page) {
                    known = t;
                    break;
                }
            }
            ordered.add(known != null ? known : register(page));
        }

        if (activeTabId != null && !tabs.containsKey(activeTabId)) {
            activeTabId = null;
        }
        return ordered;
    }

    private TabInfo snapshot(BrowserTab tab) {
        Page page = tab.getPage();
        boolean closed = page.isClosed();
        String url = "";
        String title = "";
        if (!closed) {
            try {
                url = page.url();
            } catch (RuntimeException ignored) {
            }
            try {
                title = page.title();
            } catch (RuntimeException ignored) {
            } // mid-navigation
        }
        return new TabInfo(tab.getId(), url, title, closed, tab.getId().equals(activeTabId));
    }
}
