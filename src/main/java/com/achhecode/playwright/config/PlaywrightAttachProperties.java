package com.achhecode.playwright.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Enable with @ConfigurationPropertiesScan on your main class
 * (or @EnableConfigurationProperties(PlaywrightAttachProperties.class)).
 */
@ConfigurationProperties(prefix = "playwright.attach")
public class PlaywrightAttachProperties {

    private String host = "127.0.0.1";
    private int port = 9222;

    /** Max time to wait while connecting to Chrome over CDP. */
    private double connectTimeoutMs = 10_000;
    private double defaultTimeoutMs = 30_000;
    private double navigationTimeoutMs = 60_000;

    /** Try to attach at startup. App still starts if Chrome is not running. */
    private boolean eagerConnect = true;

    // ---- used only by restartBrowser() ----
    /**
     * Full path to chrome executable, e.g. C:\\Program
     * Files\\Google\\Chrome\\Application\\chrome.exe
     */
    private String chromePath;
    /** Chrome 136+ requires a non-default profile dir for remote debugging. */
    private String userDataDir;
    private List<String> chromeArgs = new ArrayList<>();
    private long startupTimeoutMs = 15_000;

    private Capture capture = new Capture();

    public Capture getCapture() {
        return capture;
    }

    public void setCapture(Capture capture) {
        this.capture = capture;
    }

    public static class Capture {

        /**
         * Directory where captured pages are stored.
         */
        private String dir = "./captures";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public double getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(double v) {
        this.connectTimeoutMs = v;
    }

    public double getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(double v) {
        this.defaultTimeoutMs = v;
    }

    public double getNavigationTimeoutMs() {
        return navigationTimeoutMs;
    }

    public void setNavigationTimeoutMs(double v) {
        this.navigationTimeoutMs = v;
    }

    public boolean isEagerConnect() {
        return eagerConnect;
    }

    public void setEagerConnect(boolean eagerConnect) {
        this.eagerConnect = eagerConnect;
    }

    public String getChromePath() {
        return chromePath;
    }

    public void setChromePath(String chromePath) {
        this.chromePath = chromePath;
    }

    public String getUserDataDir() {
        return userDataDir;
    }

    public void setUserDataDir(String userDataDir) {
        this.userDataDir = userDataDir;
    }

    public List<String> getChromeArgs() {
        return chromeArgs;
    }

    public void setChromeArgs(List<String> chromeArgs) {
        this.chromeArgs = chromeArgs;
    }

    public long getStartupTimeoutMs() {
        return startupTimeoutMs;
    }

    public void setStartupTimeoutMs(long startupTimeoutMs) {
        this.startupTimeoutMs = startupTimeoutMs;
    }

    public String endpoint() {
        return "http://" + host + ":" + port;
    }
}
