package com.example.demo.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.WebhookResponse;

import jakarta.annotation.PostConstruct;

@Service
public class WebhookProcessor {

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        System.out.println("=== Starting WebhookProcessor ===");
        String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        // Build payload
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "REG12347");
        requestBody.put("email", "john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("\n-> Sending POST request to generate webhook...");
            System.out.println("Request URL: " + generateUrl);
            System.out.println("Request Headers: " + headers);
            System.out.println("Request Body: " + requestBody);

            ResponseEntity<WebhookResponse> response =
                restTemplate.postForEntity(generateUrl, entity, WebhookResponse.class);

            System.out.println("Received Status Code: " + response.getStatusCode());
            System.out.println("Received Headers: " + response.getHeaders());

            WebhookResponse webhookResponse = response.getBody();

            if (webhookResponse == null || webhookResponse.getWebhook() == null || webhookResponse.getAccessToken() == null) {
                System.err.println("❌ Failed to generate webhook or received null response.");
                return;
            }

            System.out.println("\n✔ Webhook URL: " + webhookResponse.getWebhook());
            System.out.println("✔ Access Token: " + webhookResponse.getAccessToken());

            // Extract numeric part and get last 2 digits
            String regNo = requestBody.get("regNo");
            String digits = regNo.replaceAll("\\D", ""); // removes non-digit chars

            int lastTwoDigits = Integer.parseInt(digits.substring(digits.length() - 2));
            System.out.println("✔ Extracted last two digits from regNo: " + lastTwoDigits);

            String finalQuery = (lastTwoDigits % 2 == 0) ? getEvenQuery() : getOddQuery();

            System.out.println("\n--- Final SQL Query to submit ---");
            System.out.println(finalQuery);
            System.out.println("--------------------------------\n");

            sendFinalAnswer(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), finalQuery);

        } catch (Exception e) {
            System.err.println("❌ Error during webhook generation or submission:");
            e.printStackTrace();
        }
        System.out.println("=== WebhookProcessor finished ===");
    }

    private String getOddQuery() {
        return "SELECT DISTINCT c.name " +
               "FROM customers c " +
               "JOIN orders o ON c.customer_id = o.customer_id " +
               "JOIN order_items oi ON o.order_id = oi.order_id " +
               "JOIN products p ON oi.product_id = p.product_id " +
               "WHERE p.name = 'Laptop' " +
               "ORDER BY c.name;";
    }

    private String getEvenQuery() {
        return "SELECT p.name, COUNT(*) AS total_orders " +
               "FROM products p " +
               "JOIN order_items oi ON p.product_id = oi.product_id " +
               "GROUP BY p.name " +
               "ORDER BY total_orders DESC " +
               "LIMIT 1;";
    }

    private void sendFinalAnswer(String webhookUrl, String accessToken, String finalQuery) {
        try {
            System.out.println("-> Sending final answer to webhook...");
            System.out.println("Submission URL: " + webhookUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Bearer " + accessToken); // JWT header

            Map<String, String> body = new HashMap<>();
            body.put("finalQuery", finalQuery);

            System.out.println("Request Headers: " + headers);
            System.out.println("Request Body: " + body);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);

            System.out.println("✔ Submission Status: " + response.getStatusCode());
            System.out.println("✔ Response Headers: " + response.getHeaders());
            System.out.println("✔ Response Body: " + response.getBody());

        } catch (Exception e) {
            System.err.println("❌ Error submitting final SQL query:");
            e.printStackTrace();
        }
    }
}
