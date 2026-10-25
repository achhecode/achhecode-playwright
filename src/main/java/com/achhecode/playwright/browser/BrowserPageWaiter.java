package com.achhecode.playwright.browser;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Component;

@Component
public class BrowserPageWaiter {

    public void waitUntilDocumentLoaded(Page page) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForLoadState(LoadState.LOAD);
    }

    /** Use sparingly: never settles on pages with long polling/websockets. */
    public void waitUntilNetworkIdle(Page page) {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
