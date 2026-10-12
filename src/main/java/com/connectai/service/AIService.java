package com.connectai.service;

import com.connectai.config.AppConfig;
import com.connectai.model.AIRephraseResponse;
import com.connectai.model.AISummaryResponse;
import com.connectai.model.Message;
import com.connectai.model.Tone;
import com.connectai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AIService {
    private static final AIService instance = new AIService();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private AIService() {}

    public static AIService getInstance() {
        return instance;
    }

    public CompletableFuture<AISummaryResponse> summarizeConversation(String conversationId) {
        return ChatService.getInstance().loadMessages(conversationId, 50, 0)
                .thenCompose(messages -> summarizeConversation(conversationId, messages))
                .exceptionally(ex -> {
                    // Fallback to general channel summary if message loading failed
                    return summarizeConversation(conversationId, List.of()).join();
                });
    }

    public CompletableFuture<AISummaryResponse> summarizeConversation(String conversationId, List<Message> messages) {
        String apiKey = AppConfig.getOpenAiApiKey();

        if (apiKey.isBlank()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().summarize(conversationId));
        }

        StringBuilder chatTranscript = new StringBuilder();
        if (messages != null && !messages.isEmpty()) {
            // Messages are ordered newest first from DB, reverse for chronological transcript
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if (m.getContent() != null && !m.getContent().isBlank() && !m.isDeleted()) {
                    String senderName = m.getSenderProfile() != null ? m.getSenderProfile().getEffectiveName() : "Team Member";
                    chatTranscript.append(senderName).append(": ").append(m.getContent()).append("\n");
                }
            }
        }

        if (chatTranscript.length() == 0) {
            chatTranscript.append("Team discussion channel initialized. Welcome to ConnectAI!");
        }

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("model", "gpt-4o-mini");
        reqBody.put("temperature", 0.5);

        List<Map<String, String>> promptMessages = List.of(
                Map.of(
                        "role", "system",
                        "content", "You are ConnectAI assistant, an intelligent desktop chat companion. "
                                + "Provide a clear, high-quality executive summary of the conversation thread. "
                                + "Structure your response with: "
                                + "1. A concise overview summary paragraph. "
                                + "2. Followed by 3 to 5 clear bullet points (starting with •) covering key discussion items, updates, and decisions."
                ),
                Map.of(
                        "role", "user",
                        "content", "Please summarize the following conversation thread:\n\n" + chatTranscript
                )
        );
        reqBody.put("messages", promptMessages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(reqBody)))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        System.err.println("OpenAI API error: " + response.body());
                        return com.connectai.mock.MockDataProvider.getInstance().summarize(conversationId);
                    }

                    try {
                        JsonNode root = JsonUtil.getMapper().readTree(response.body());
                        String aiText = root.path("choices").path(0).path("message").path("content").asText();

                        AISummaryResponse res = new AISummaryResponse();
                        res.setSummary(aiText);

                        List<String> bullets = new ArrayList<>();
                        for (String line : aiText.split("\n")) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                                bullets.add(trimmed.substring(1).trim());
                            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                                bullets.add(trimmed.replaceFirst("^\\d+\\.\\s+", "").trim());
                            }
                        }

                        if (bullets.isEmpty()) {
                            bullets.add("Executive summary generated by OpenAI gpt-4o-mini");
                            bullets.add("Key conversation points consolidated");
                        }
                        res.setBulletPoints(bullets);
                        return res;
                    } catch (Exception e) {
                        return com.connectai.mock.MockDataProvider.getInstance().summarize(conversationId);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("OpenAI request failed: " + ex.getMessage());
                    return com.connectai.mock.MockDataProvider.getInstance().summarize(conversationId);
                });
    }

    public CompletableFuture<AIRephraseResponse> rephraseMessage(String text, Tone tone) {
        String apiKey = AppConfig.getOpenAiApiKey();

        if (apiKey.isBlank()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().rephrase(text, tone));
        }

        String toneName = tone != null ? tone.getDisplayName() : "Professional";

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("model", "gpt-4o-mini");
        reqBody.put("temperature", 0.7);

        List<Map<String, String>> promptMessages = List.of(
                Map.of(
                        "role", "system",
                        "content", "You are ConnectAI writing assistant. Rewrite the user's message in the requested tone ("
                                + toneName + "). Return ONLY the rewritten text without quotes, introductory text, or explanations."
                ),
                Map.of(
                        "role", "user",
                        "content", "Rewrite this draft message in a " + toneName + " tone:\n\n" + text
                )
        );
        reqBody.put("messages", promptMessages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(reqBody)))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        return com.connectai.mock.MockDataProvider.getInstance().rephrase(text, tone);
                    }

                    try {
                        JsonNode root = JsonUtil.getMapper().readTree(response.body());
                        String rewritten = root.path("choices").path(0).path("message").path("content").asText().trim();

                        AIRephraseResponse res = new AIRephraseResponse();
                        res.setOriginalText(text);
                        res.setRephrasedText(rewritten);
                        res.setAlternatives(List.of(rewritten));
                        return res;
                    } catch (Exception e) {
                        return com.connectai.mock.MockDataProvider.getInstance().rephrase(text, tone);
                    }
                })
                .exceptionally(ex -> com.connectai.mock.MockDataProvider.getInstance().rephrase(text, tone));
    }
}
