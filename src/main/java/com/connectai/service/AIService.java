package com.connectai.service;

import com.connectai.config.AppConfig;
import com.connectai.model.AIRephraseResponse;
import com.connectai.model.AISummaryResponse;
import com.connectai.model.Message;
import com.connectai.model.Tone;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Routes authenticated AI requests through the Supabase Edge Function. */
public class AIService {
    private static final AIService instance = new AIService();

    private AIService() {}

    public static AIService getInstance() { return instance; }

    public CompletableFuture<AISummaryResponse> summarizeConversation(String conversationId) {
        return ChatService.getInstance().loadMessages(conversationId, 50, 0)
                .thenCompose(messages -> summarizeConversation(conversationId, messages));
    }

    public CompletableFuture<AISummaryResponse> summarizeConversation(String conversationId, List<Message> messages) {
        if (AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().summarize(conversationId));
        }
        StringBuilder transcript = new StringBuilder();
        if (messages != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message == null || message.isDeleted() || message.getContent() == null || message.getContent().isBlank()) continue;
                String sender = message.getSenderProfile() == null ? "Team Member" : message.getSenderProfile().getEffectiveName();
                transcript.append(sender).append(": ").append(message.getContent()).append('\n');
            }
        }
        if (transcript.toString().isBlank())
            return CompletableFuture.failedFuture(new IllegalStateException("There are no recent messages to summarize."));

        Map<String, Object> request = new HashMap<>();
        request.put("action", "summarize");
        request.put("conversationId", conversationId);
        request.put("messagesText", transcript.toString());
        return authenticatedFunctionCall(request).thenApply(body -> {
            try {
                AISummaryResponse summary = JsonUtil.getMapper().treeToValue(body, AISummaryResponse.class);
                if (summary.getSummary() == null || summary.getSummary().isBlank())
                    throw new IllegalStateException("The AI service returned an empty summary.");
                summary.setBulletPoints(summary.getKeyDecisions());
                return summary;
            } catch (Exception error) {
                throw new java.util.concurrent.CompletionException(error);
            }
        });
    }

    public CompletableFuture<AIRephraseResponse> rephraseMessage(String text, Tone tone) {
        if (text == null || text.isBlank())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Type a draft before asking AI to rephrase it."));
        if (AppConfig.isMockMode())
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().rephrase(text, tone));

        Map<String, Object> request = new HashMap<>();
        request.put("action", "rephrase");
        request.put("text", text);
        request.put("tone", tone == null ? "Professional" : tone.getDisplayName());
        return authenticatedFunctionCall(request).thenApply(body -> {
            String rewritten = body.path("rephrasedText").asText("").trim();
            if (rewritten.isBlank()) throw new IllegalStateException("The AI service returned an empty rewrite.");
            AIRephraseResponse result = new AIRephraseResponse();
            result.setOriginalText(text);
            result.setRephrasedText(rewritten);
            result.setAlternatives(List.of(rewritten));
            return result;
        });
    }

    private CompletableFuture<JsonNode> authenticatedFunctionCall(Map<String, Object> payload) {
        String token = AuthService.getInstance().getAccessToken();
        if (token == null || token.isBlank())
            return CompletableFuture.failedFuture(new IllegalStateException("Sign in to use AI assistance."));
        if (!AppConfig.isSupabaseConfigured())
            return CompletableFuture.failedFuture(new IllegalStateException("Configure Supabase to use AI assistance."));
        return HttpClientUtil.postAsync("/functions/v1/ai-assistant", JsonUtil.toJson(payload), token)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        String detail = "AI request failed (HTTP " + response.statusCode() + ").";
                        try { detail = JsonUtil.getMapper().readTree(response.body()).path("error").asText(detail); }
                        catch (Exception ignored) {}
                        throw new IllegalStateException(detail);
                    }
                    try { return JsonUtil.getMapper().readTree(response.body()); }
                    catch (Exception error) { throw new IllegalStateException("The AI service returned an invalid response.", error); }
                });
    }
}
