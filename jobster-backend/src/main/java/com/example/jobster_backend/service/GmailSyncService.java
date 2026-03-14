package com.example.jobster_backend.service;

import com.example.jobster_backend.dto.GmailSyncRequest;
import com.example.jobster_backend.dto.GmailSyncResponse;
import com.example.jobster_backend.entity.Job;
import com.example.jobster_backend.entity.User;
import com.example.jobster_backend.repository.JobRepository;
import com.example.jobster_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailSyncService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    private static final String GMAIL_API_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final int BATCH_SIZE = 10;
    private static final int MAX_EMAILS = 100;

    public GmailSyncResponse syncEmails(GmailSyncRequest request, User user) {

        String token = request.getGmailAccessToken();
        HttpHeaders headers = bearerHeaders(token);

        boolean firstSync = (user.getEmailSyncStartDate() == null);
        if (firstSync) {
            user.setEmailSyncStartDate(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
            user.setGmailEmail(request.getUserEmail());
            userRepository.save(user);
        }

        long afterEpoch = user.getEmailSyncStartDate().toEpochSecond(ZoneOffset.UTC);
        String query = "after:" + afterEpoch + " -from:me";

        List<String> messageIds = listMessageIds(headers, query);
        log.info("Found {} Gmail messages for user {}", messageIds.size(), user.getEmail());

        int processed = 0, categorized = 0, skipped = 0;

        for (int i = 0; i < Math.min(messageIds.size(), MAX_EMAILS); i += BATCH_SIZE) {
            List<String> batch = messageIds.subList(i, Math.min(i + BATCH_SIZE, messageIds.size()));
            List<EmailData> emails = new ArrayList<>();

            for (String msgId : batch) {
                if (jobRepository.existsByGmailMessageId(msgId)) {
                    skipped++;
                    continue;
                }
                EmailData email = fetchEmailData(headers, msgId);
                if (email != null) {
                    emails.add(email);
                    processed++;
                }
            }

            if (emails.isEmpty()) continue;

            List<ClaudeResult> results = categorizeBatch(emails);

            for (ClaudeResult result : results) {
                if (result.isJobRelated()) {
                    try {
                        Job job = Job.builder()
                                .user(user)
                                .company(result.getCompany() != null ? result.getCompany() : "Unknown")
                                .position(result.getPosition() != null ? result.getPosition() : "Unknown")
                                .jobLocation(result.getJobLocation() != null ? result.getJobLocation() : "Not specified")
                                .jobType("full-time")
                                .status(result.getStatus() != null ? result.getStatus() : Job.JobStatus.APPLIED)
                                .gmailMessageId(result.getMessageId())
                                .emailSubject(result.getSubject())
                                .emailPreview(result.getPreview())
                                .emailDate(result.getEmailDate())
                                .build();
                        jobRepository.save(job);   // ← THIS was missing
                        categorized++;
                        log.info("Saved: {} at {}", job.getPosition(), job.getCompany());
                    } catch (Exception e) {
                        log.error("Save failed: {}", e.getMessage());
                    }
                }
            }
        }

        GmailSyncResponse.SyncStats stats = GmailSyncResponse.SyncStats.builder()
                .applied(jobRepository.countByUserAndStatus(user, Job.JobStatus.APPLIED))
                .interviews(jobRepository.countByUserAndStatus(user, Job.JobStatus.INTERVIEW))
                .offers(jobRepository.countByUserAndStatus(user, Job.JobStatus.OFFER))
                .rejected(jobRepository.countByUserAndStatus(user, Job.JobStatus.REJECTED))
                .build();

        return GmailSyncResponse.builder()
                .processed(processed)
                .categorized(categorized)
                .skipped(skipped)
                .stats(stats)
                .build();
    }

    // ─── Gmail: list message IDs ────────────────────────────────
    private List<String> listMessageIds(HttpHeaders headers, String query) {
        List<String> ids = new ArrayList<>();
        try {
            String url = GMAIL_API_BASE + "/messages?maxResults=100&q=" + query;
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(res.getBody());
            JsonNode messages = root.path("messages");
            if (messages.isArray()) {
                for (JsonNode msg : messages) {
                    ids.add(msg.path("id").asText());
                }
            }
        } catch (Exception e) {
            log.error("Failed to list Gmail messages: {}", e.getMessage());
        }
        return ids;
    }

    // ─── Gmail: fetch single email ──────────────────────────────
    private EmailData fetchEmailData(HttpHeaders headers, String messageId) {
        try {
            String url = GMAIL_API_BASE + "/messages/" + messageId + "?format=full";
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(res.getBody());
            JsonNode payload = root.path("payload");
            JsonNode hdrs = payload.path("headers");

            String subject = "", from = "";
            long internalDate = root.path("internalDate").asLong(0);

            for (JsonNode h : hdrs) {
                String name = h.path("name").asText();
                if ("Subject".equalsIgnoreCase(name)) subject = h.path("value").asText();
                if ("From".equalsIgnoreCase(name))    from    = h.path("value").asText();
            }

            String body = extractBody(payload);
            String preview = body.length() > 200 ? body.substring(0, 200) : body;

            LocalDateTime emailDate = internalDate > 0
                    ? LocalDateTime.ofEpochSecond(internalDate / 1000, 0, ZoneOffset.UTC)
                    : LocalDateTime.now(ZoneOffset.UTC);

            return new EmailData(messageId, subject, from, preview, emailDate);

        } catch (Exception e) {
            log.error("Failed to fetch email {}: {}", messageId, e.getMessage());
            return null;
        }
    }

    private String extractBody(JsonNode payload) {
        JsonNode parts = payload.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if ("text/plain".equals(part.path("mimeType").asText())) {
                    String data = part.path("body").path("data").asText();
                    if (!data.isEmpty()) return decodeBase64(data);
                }
            }
            for (JsonNode part : parts) {
                if ("text/html".equals(part.path("mimeType").asText())) {
                    String data = part.path("body").path("data").asText();
                    if (!data.isEmpty()) return stripHtml(decodeBase64(data));
                }
            }
        }
        String data = payload.path("body").path("data").asText();
        return data.isEmpty() ? "" : decodeBase64(data);
    }

    private String decodeBase64(String encoded) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return new String(decoded).replaceAll("[\\r\\n]+", " ").trim();
        } catch (Exception e) { return ""; }
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    // ─── Claude: categorize batch ───────────────────────────────
    private List<ClaudeResult> categorizeBatch(List<EmailData> emails) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            You are analyzing emails to categorize job application activity.
            For each email below, determine:
            1. Is it job-application related? (applied, interview invite, rejection, or offer)
            2. If yes, extract: company name, position/role, location (city/remote if mentioned), and category

            Categories:
            - APPLIED: confirmation you applied to a role
            - INTERVIEW: invite for phone screen, interview, or assessment
            - REJECTED: rejection or no-longer-moving-forward email
            - OFFER: job offer letter or verbal offer

            Respond ONLY with a JSON array. Each element:
            {
              "messageId": "...",
              "isJobRelated": true/false,
              "company": "Company Name or null",
              "position": "Role Title or null",
              "jobLocation": "City, Province or Remote or null",
              "status": "APPLIED|INTERVIEW|REJECTED|OFFER or null"
            }

            Emails:
            """);

        for (int i = 0; i < emails.size(); i++) {
            EmailData e = emails.get(i);
            prompt.append(String.format(
                    "[%d] ID: %s | Subject: %s | Preview: %s\n",
                    i + 1, e.messageId(), e.subject(), e.preview()
            ));
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "claude-sonnet-4-20250514");
            body.put("max_tokens", 1000);
            body.put("messages", List.of(
                    Map.of("role", "user", "content", prompt.toString())
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            ResponseEntity<String> res = restTemplate.exchange(
                    "https://api.anthropic.com/v1/messages",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(res.getBody());
            String text = root.path("content").get(0).path("text").asText();
            text = text.replaceAll("```json|```", "").trim();
            log.info("Claude response: {}", text);

            JsonNode results = objectMapper.readTree(text);
            List<ClaudeResult> list = new ArrayList<>();

            for (JsonNode r : results) {
                String msgId = r.path("messageId").asText();
                boolean jobRelated = r.path("isJobRelated").asBoolean(false);
                if (!jobRelated) {
                    list.add(ClaudeResult.notJobRelated(msgId));
                    continue;
                }

                Job.JobStatus status;
                try {
                    status = Job.JobStatus.valueOf(r.path("status").asText("APPLIED"));
                } catch (IllegalArgumentException ex) {
                    status = Job.JobStatus.APPLIED;
                }

                String jobLocation = r.path("jobLocation").isNull() || r.path("jobLocation").isMissingNode()
                        ? "Not specified"
                        : r.path("jobLocation").asText("Not specified");

                EmailData original = emails.stream()
                        .filter(e -> e.messageId().equals(msgId))
                        .findFirst().orElse(emails.get(0));

                list.add(ClaudeResult.builder()
                        .messageId(msgId)
                        .jobRelated(true)
                        .company(r.path("company").asText("Unknown"))
                        .position(r.path("position").asText("Unknown"))
                        .jobLocation(jobLocation)
                        .status(status)
                        .subject(original.subject())
                        .preview(original.preview())
                        .emailDate(original.emailDate())
                        .build());
            }
            return list;

        } catch (Exception e) {
            log.error("Claude categorization failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ─── Internal data models ───────────────────────────────────
    record EmailData(String messageId, String subject, String from,
                     String preview, LocalDateTime emailDate) {}

    @lombok.Data
    @lombok.Builder
    static class ClaudeResult {
        private String messageId;
        private boolean jobRelated;
        private String company;
        private String position;
        private String jobLocation;
        private Job.JobStatus status;
        private String subject;
        private String preview;
        private LocalDateTime emailDate;

        static ClaudeResult notJobRelated(String id) {
            return ClaudeResult.builder().messageId(id).jobRelated(false).build();
        }
    }
}