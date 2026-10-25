package com.achhecode.playwright.browser;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Playwright for Java is NOT thread-safe: every object (Playwright, Browser,
 * BrowserContext, Page) must be used from the thread that created Playwright.
 * Tomcat request threads differ from the startup thread, so ALL Playwright work
 * is funnelled through this single dedicated thread.
 *
 * Re-entrant: calling call() from the worker thread runs the task inline.
 */
@Component
public class PlaywrightExecutor {

    private static final long TIMEOUT_SECONDS = 120;

    private volatile Thread worker;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "playwright-worker");
        t.setDaemon(true);
        worker = t;
        return t;
    });

    public <T> T call(Callable<T> task) {
        if (Thread.currentThread() == worker) {
            return invoke(task);
        }

        Future<T> future = executor.submit(task);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(false);
            throw new IllegalStateException("Interrupted while waiting for the browser.", e);
        } catch (TimeoutException e) {
            future.cancel(false);
            throw new IllegalStateException("Browser operation timed out.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re)
                throw re;
            if (cause instanceof Error err)
                throw err;
            throw new IllegalStateException(cause);
        }
    }

    public void run(Runnable task) {
        call(() -> {
            task.run();
            return null;
        });
    }

    private <T> T invoke(Callable<T> task) {
        try {
            return task.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
