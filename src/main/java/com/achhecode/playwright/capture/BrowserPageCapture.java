package com.achhecode.playwright.capture;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.achhecode.playwright.browser.PlaywrightManager;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

@Component
public class BrowserPageCapture {

	private final PlaywrightManager playwrightManager;

	public BrowserPageCapture(PlaywrightManager playwrightManager) {
		this.playwrightManager = playwrightManager;
	}

	public Optional<CapturedPage> capture() {

		BrowserContext context = playwrightManager.getContext();

		if (context.pages().isEmpty()) {
			return Optional.empty();
		}

		Page page = getActivePage(context);

		String html = page.content();
		byte[] screenshot = page.screenshot(
				new Page.ScreenshotOptions().setFullPage(true));

		return Optional.of(
				new CapturedPage(page.url(), page.title(), html, screenshot));
	}

	private Page getActivePage(BrowserContext context) {
		// For now, use the last page.
		return context.pages().get(context.pages().size() - 1);
	}

	public record CapturedPage(
			String url,
			String title,
			String html,
			byte[] screenshot) {
	}
}
