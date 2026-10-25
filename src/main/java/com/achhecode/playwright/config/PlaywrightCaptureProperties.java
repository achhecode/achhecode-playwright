
package com.achhecode.playwright.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "playwright.capture")
public class PlaywrightCaptureProperties {

    private String dir = "./captures";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
