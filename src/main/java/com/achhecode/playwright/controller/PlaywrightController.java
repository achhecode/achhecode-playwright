package com.achhecode.playwright.controller;

import com.achhecode.playwright.browser.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController

@RequestMapping("/api/playwright")
public class PlaywrightController {

    private final PlaywrightManager playwrightManager;
    private final TabManager tabManager;
    private final BrowserNavigationService navigationService;

    public PlaywrightController(PlaywrightManager playwrightManager, TabManager tabManager,
            BrowserNavigationService navigationService) {
        this.playwrightManager = playwrightManager;
        this.tabManager = tabManager;
        this.navigationService = navigationService;
    }

    // ---- backward-compatible endpoints (operate on the active tab) ----

    @GetMapping("/url")
    public Map<String, String> getCurrentUrl() {
        return Map.of("url", tabManager.describe(tabManager.getActiveTab()).url());
    }

    @GetMapping("/title")
    public Map<String, String> getPageTitle() {
        return Map.of("title", tabManager.describe(tabManager.getActiveTab()).title());
    }

    @PostMapping("/navigate")
    public TabInfo navigate(@RequestParam String url, @RequestParam(required = false) String tabId) {
        return navigationService.navigate(tabId, url);
    }

    // ---- browser / tabs ----

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("connected", playwrightManager.isConnected());
    }

    @PostMapping("/reconnect")
    public Map<String, Object> reconnect() {
        playwrightManager.reconnect();
        return Map.of("connected", true);
    }

    @GetMapping("/tabs")
    public List<TabInfo> tabs() {
        return tabManager.listTabs();
    }

    @PostMapping("/tabs")

    @ResponseStatus(HttpStatus.CREATED)
    public TabInfo createTab() {
        return tabManager.describe(tabManager.createTab());
    }

    @PostMapping("/tabs/{tabId}/activate")
    public TabInfo activate(@PathVariable String tabId) {
        return tabManager.describe(tabManager.activateTab(tabId));
    }

    @DeleteMapping("/tabs/{tabId}")

    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String tabId) {
        tabManager.closeTab(tabId);
    }

    // Soft reset: close all tabs, keep Chrome, leave one blank tab.

    @PostMapping("/tabs/reset")
    public TabInfo resetTabs() {
        return tabManager.describe(tabManager.resetTabs());
    }

    // Hard restart: close Chrome completely, launch a fresh one, re-attach.

    @PostMapping("/restart")
    public Map<String, Object> restartBrowser() {
        playwrightManager.restartBrowser();
        return Map.of("restarted", true, "tabs", tabManager.listTabs());
    }

    @PostMapping("/tabs/{tabId}/reload")
    public TabInfo reload(@PathVariable String tabId) {
        return navigationService.reload(tabId);
    }
}
