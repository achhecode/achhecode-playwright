package com.achhecode.playwright.browser;

/** Thread-safe snapshot of a tab, safe to return from controllers. */
public record TabInfo(String id, String url, String title, boolean closed, boolean active) {
}
