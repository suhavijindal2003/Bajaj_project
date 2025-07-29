package com.example.demo.dto;

public class WebhookResponse {
    private String accessToken;
    private String webhook;

    public WebhookResponse() {}

    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public String getWebhook() {
        return webhook;
    }
    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }
}
