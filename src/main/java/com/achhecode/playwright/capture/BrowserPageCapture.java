package com.achhecode.playwright.capture;

import com.achhecode.playwright.browser.PlaywrightManager;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

@Component
public class BrowserPageCapture {

	private final PlaywrightManager playwrightManager;

	public BrowserPageCapture(PlaywrightManager playwrightManager) {
		this.playwrightManager = playwrightManager;
	}

	public CapturedPage capture() {

		BrowserContext context = playwrightManager.getContext();

		if (context.pages().isEmpty()) {
			throw new IllegalStateException("No open browser page found.");
		}

		Page page = getActivePage(context);

		String html = page.content();
		byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));

		return new CapturedPage(page.url(), page.title(), html, screenshot);
	}

	private Page getActivePage(BrowserContext context) {

		// For now use the last page.
		// We can improve active-tab detection later.
		return context.pages().get(context.pages().size() - 1);
	}

	public record CapturedPage(String url, String title, String html, byte[] screenshot) {
	}
}