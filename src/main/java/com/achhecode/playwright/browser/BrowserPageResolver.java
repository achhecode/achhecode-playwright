package com.achhecode.playwright.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.springframework.stereotype.Component;

/**
 * Finds / opens raw Playwright pages. Use the returned Page only inside
 * PlaywrightExecutor.call(...).
 */
@Component
public class BrowserPageResolver {

    private final PlaywrightManager playwrightManager;
    private final PlaywrightExecutor executor;

    public BrowserPageResolver(PlaywrightManager playwrightManager, PlaywrightExecutor executor) {
        this.playwrightManager = playwrightManager;
        this.executor = executor;
    }

    /** Reuses a tab already on this URL prefix, else navigates a blank/new tab. */
    public Page findOrOpenPage(String url) {
        return executor.call(() -> {
            BrowserContext context = playwrightManager.getContext();
            return context.pages().stream().filter(p -> !p.isClosed()).filter(p -> safeUrl(p).startsWith(url))
                    .findFirst().orElseGet(() -> {
                        Page page = getFreshPage();
                        page.navigate(url);
                        return page;
                    });
        });
    }

    /** First open tab, or a new one. */
    public Page getOrCreatePage() {
        return executor.call(() -> {
            BrowserContext context = playwrightManager.getContext();
            return context.pages().stream().filter(p -> !p.isClosed()).findFirst().orElseGet(context::newPage);
        });
    }

    /** A blank tab if one exists (website tabs are preserved), else a new tab. */
    public Page getFreshPage() {
        return executor.call(() -> {
            BrowserContext context = playwrightManager.getContext();
            return context.pages().stream().filter(p -> !p.isClosed()).filter(this::isBlankPage).findFirst()
                    .orElseGet(context::newPage);
        });
    }

    /** Most recently opened tab, or a new one. */
    public Page getLatestPage() {
        return executor.call(() -> {
            BrowserContext context = playwrightManager.getContext();
            return context.pages().stream().filter(p -> !p.isClosed()).reduce((first, second) -> second)
                    .orElseGet(context::newPage);
        });
    }

    private boolean isBlankPage(Page page) {
        String url = safeUrl(page);
        return url.isEmpty() || url.startsWith("about:blank") || url.startsWith("chrome://newtab");
    }

    private String safeUrl(Page page) {
        try {
            return page.url();
        } catch (PlaywrightException e) {
            return "";
        }
    }
}
