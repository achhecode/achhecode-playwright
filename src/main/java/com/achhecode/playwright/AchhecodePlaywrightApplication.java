package com.achhecode.playwright;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import com.achhecode.playwright.config.PlaywrightAttachProperties;

@SpringBootApplication
@ComponentScan(basePackages = "com.achhecode.playwrightcore")
@EnableConfigurationProperties(PlaywrightAttachProperties.class)
public class AchhecodePlaywrightApplication {

	public static void main(String[] args) {
		SpringApplication.run(AchhecodePlaywrightApplication.class, args);
	}

}
