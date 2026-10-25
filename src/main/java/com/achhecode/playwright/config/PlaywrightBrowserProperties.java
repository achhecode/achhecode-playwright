
package com.achhecode.playwright.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "playwright.browser")
public class PlaywrightBrowserProperties {

    public enum Mode {
        HEADLESS,
        ATTACHED,
        LAUNCH
    }

    private Mode mode = Mode.HEADLESS;
    private boolean headless = true;
    private boolean eagerConnect = true;

    private String executablePath;

    private String host = "127.0.0.1";
    private int port = 9222;

    private double connectTimeoutMs = 10_000;
    private double defaultTimeoutMs = 30_000;
    private double navigationTimeoutMs = 60_000;

    private long startupTimeoutMs = 15_000;

    private String userDataDir;
    private List<String> args = new ArrayList<>();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isEagerConnect() {
        return eagerConnect;
    }

    public void setEagerConnect(boolean eagerConnect) {
        this.eagerConnect = eagerConnect;
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
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

    public void setConnectTimeoutMs(double connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public double getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(double defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public double getNavigationTimeoutMs() {
        return navigationTimeoutMs;
    }

    public void setNavigationTimeoutMs(double navigationTimeoutMs) {
        this.navigationTimeoutMs = navigationTimeoutMs;
    }

    public long getStartupTimeoutMs() {
        return startupTimeoutMs;
    }

    public void setStartupTimeoutMs(long startupTimeoutMs) {
        this.startupTimeoutMs = startupTimeoutMs;
    }

    public String getUserDataDir() {
        return userDataDir;
    }

    public void setUserDataDir(String userDataDir) {
        this.userDataDir = userDataDir;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public String endpoint() {
        return "http://" + host + ":" + port;
    }
}
