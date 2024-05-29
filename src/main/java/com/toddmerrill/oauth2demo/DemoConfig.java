package com.toddmerrill.oauth2demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DemoConfig {

    @Value("${oauth.base.url}")
    private String oauthBaseUrl;

    @Value("${resource.base.url}")
    private String resourceBaseUrl;

    @Bean
    @Qualifier("oauthClient")
    public WebClient oauthClient() {
        return WebClient.builder()
                .baseUrl(oauthBaseUrl)
                .build();
    }

    @Bean
    @Qualifier("resourceClient")
    public WebClient resourceClient() {
        return WebClient
                .builder()
                .baseUrl(resourceBaseUrl)
                .defaultHeaders(
                        httpHeaders -> {
                            httpHeaders.set("Accept", "application/vnd.github+json");
                            httpHeaders.set("User-Agent", "Oauth2-Demo-App");
                        })
                .build();
    }

}