package com.achhecode.playwright.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.achhecode.playwright.browser.BrowserNavigationService;
import com.achhecode.playwright.browser.PlaywrightManager;
import com.achhecode.playwright.browser.TabInfo;
import com.achhecode.playwright.browser.TabManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/playwright")
@Tag(name = "Playwright Browser Management", description = "APIs for browser lifecycle management, page navigation, and browser tab operations.")
public class PlaywrightController {

    private final PlaywrightManager playwrightManager;
    private final TabManager tabManager;
    private final BrowserNavigationService navigationService;

    public PlaywrightController(
            PlaywrightManager playwrightManager,
            TabManager tabManager,
            BrowserNavigationService navigationService) {
        this.playwrightManager = playwrightManager;
        this.tabManager = tabManager;
        this.navigationService = navigationService;
    }

    // ---- Backward-compatible endpoints (operate on the active tab) ----

    @GetMapping("/url")
    @Operation(summary = "Get current page URL", description = "Returns the URL of the currently active browser tab.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current page URL retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve the current page URL")
    })
    public Map<String, String> getCurrentUrl() {
        return Map.of("url", tabManager.describe(tabManager.getActiveTab()).url());
    }

    @GetMapping("/title")
    @Operation(summary = "Get current page title", description = "Returns the title of the page loaded in the currently active browser tab.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page title retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve the page title")
    })
    public Map<String, String> getPageTitle() {
        return Map.of("title", tabManager.describe(tabManager.getActiveTab()).title());
    }

    @PostMapping("/navigate")
    @Operation(summary = "Navigate to a URL", description = "Navigates the specified browser tab to the given URL. "
            + "If tabId is omitted, navigation is performed on the active tab.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Navigation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URL or navigation parameters"),
            @ApiResponse(responseCode = "500", description = "Navigation failed")
    })
    public TabInfo navigate(
            @Parameter(description = "Destination URL, including its scheme (for example, https://example.com)", required = true) @RequestParam String url,
            @Parameter(description = "ID of the tab to navigate. Defaults to the active tab when omitted.") @RequestParam(required = false) String tabId) {
        return navigationService.navigate(tabId, url);
    }

    // ---- Browser status and lifecycle ----

    @GetMapping("/status")
    @Operation(summary = "Get browser connection status", description = "Checks whether the Playwright browser connection is currently established.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser connection status retrieved successfully")
    })
    public Map<String, Object> status() {
        return Map.of("connected", playwrightManager.isConnected());
    }

    @PostMapping("/reconnect")
    @Operation(summary = "Reconnect to the browser", description = "Attempts to restore the Playwright connection to the browser.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser reconnection completed"),
            @ApiResponse(responseCode = "500", description = "Browser reconnection failed")
    })
    public Map<String, Object> reconnect() {
        playwrightManager.reconnect();
        return Map.of("connected", true);
    }

    @PostMapping("/restart")
    @Operation(summary = "Restart the browser", description = "Performs a hard restart by closing the current browser, launching a fresh browser, "
            + "reattaching Playwright, and returning the resulting browser tab list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser restarted successfully"),
            @ApiResponse(responseCode = "500", description = "Browser restart failed")
    })
    public Map<String, Object> restartBrowser() {
        playwrightManager.restartBrowser();
        return Map.of("restarted", true, "tabs", tabManager.listTabs());
    }

    // ---- Browser tabs ----

    @GetMapping("/tabs")
    @Operation(summary = "List browser tabs", description = "Returns information about all browser tabs currently managed by the application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser tabs retrieved successfully")
    })
    public List<TabInfo> tabs() {
        return tabManager.listTabs();
    }

    @PostMapping("/tabs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a browser tab", description = "Creates a new browser tab and returns its information.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Browser tab created successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to create a browser tab")
    })
    public TabInfo createTab() {
        return tabManager.describe(tabManager.createTab());
    }

    @PostMapping("/tabs/{tabId}/activate")
    @Operation(summary = "Activate a browser tab", description = "Sets the specified browser tab as the active tab and returns its information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser tab activated successfully"),
            @ApiResponse(responseCode = "404", description = "Browser tab not found"),
            @ApiResponse(responseCode = "500", description = "Failed to activate the browser tab")
    })
    public TabInfo activate(
            @Parameter(description = "ID of the browser tab to activate", required = true) @PathVariable String tabId) {
        return tabManager.describe(tabManager.activateTab(tabId));
    }

    @DeleteMapping("/tabs/{tabId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a browser tab", description = "Closes the specified browser tab and returns no response body.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Browser tab closed successfully"),
            @ApiResponse(responseCode = "404", description = "Browser tab not found"),
            @ApiResponse(responseCode = "500", description = "Failed to close the browser tab")
    })
    public void close(
            @Parameter(description = "ID of the browser tab to close", required = true) @PathVariable String tabId) {
        tabManager.closeTab(tabId);
    }

    @PostMapping("/tabs/reset")
    @Operation(summary = "Reset browser tabs", description = "Performs a soft reset by closing all managed tabs while keeping the browser running, "
            + "then creates or leaves one blank tab and returns its information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser tabs reset successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to reset browser tabs")
    })
    public TabInfo resetTabs() {
        return tabManager.describe(tabManager.resetTabs());
    }

    @PostMapping("/tabs/{tabId}/reload")
    @Operation(summary = "Reload a browser tab", description = "Reloads the page in the specified browser tab and returns its updated information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browser tab reloaded successfully"),
            @ApiResponse(responseCode = "404", description = "Browser tab not found"),
            @ApiResponse(responseCode = "500", description = "Failed to reload the browser tab")
    })
    public TabInfo reload(
            @Parameter(description = "ID of the browser tab to reload", required = true) @PathVariable String tabId) {
        return navigationService.reload(tabId);
    }
}
