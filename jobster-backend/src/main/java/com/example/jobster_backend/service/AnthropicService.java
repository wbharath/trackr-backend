package com.example.jobster_backend.service;

import com.example.jobster_backend.dto.ExtractResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class AnthropicService {
    @Value("${anthropic.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractResponseDto extractJobDetails(String pageText, String pageTitle) {
        try {
            String prompt = String.format(
                    "Extract job details from this job posting. Return ONLY a JSON object with keys: position, company, jobLocation. Use empty string if not found.\n\nPage title: %s\nPage content: %s\n\nReturn only JSON. Example: {\"position\": \"Software Engineer\", \"company\": \"Google\", \"jobLocation\": \"Toronto, ON\"}",
                    pageTitle, pageText
            );

            String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("model", "claude-haiku-4-5-20251001");
                put("max_tokens", 200);
                put("messages", new java.util.ArrayList<>() {{
                    add(new java.util.HashMap<>() {{
                        put("role", "user");
                        put("content", prompt);
                    }});
                }});
            }});

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("=== ANTHROPIC RAW RESPONSE ===");
            System.out.println("Status: " + response.statusCode());
            System.out.println("Body: " + response.body());
            JsonNode root = objectMapper.readTree(response.body());

            String content = root.path("content").get(0).path("text").asText();
            String clean = content.replaceAll("```json|```", "").trim();

            JsonNode extracted = objectMapper.readTree(clean);

            return new ExtractResponseDto(
                    extracted.path("position").asText(""),
                    extracted.path("company").asText(""),
                    extracted.path("jobLocation").asText("")
            );

        } catch (Exception e) {
            return new ExtractResponseDto("", "", "");
        }
    }
}
