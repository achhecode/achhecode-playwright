package com.achhecode.playwright.browser;

import com.microsoft.playwright.Page;

/**
 * Registry entry: stable id + Page. Touch the Page ONLY inside
 * PlaywrightExecutor.call(...).
 */
public class BrowserTab {

    private final String id;
    private final Page page;

    public BrowserTab(String id, Page page) {
        this.id = id;
        this.page = page;
    }

    public String getId() {
        return id;
    }

    public Page getPage() {
        return page;
    }
}
