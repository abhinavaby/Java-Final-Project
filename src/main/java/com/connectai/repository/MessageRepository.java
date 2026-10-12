package com.connectai.repository;

import com.connectai.model.Message;
import com.connectai.model.MessageReaction;
import com.connectai.model.MessageReceipt;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MessageRepository {

    public CompletableFuture<List<Message>> getMessages(String conversationId, int limit, int offset, String accessToken) {
        String path = "/rest/v1/messages?conversation_id=eq." + conversationId +
                "&select=*,sender_profile:profiles!messages_sender_id_fkey(*),attachment:attachments(*),poll:polls(*,options:poll_options(*)),reactions:message_reactions(*)" +
                "&order=created_at.desc&limit=" + limit + "&offset=" + offset;

        return HttpClientUtil.getAsync(path, accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to fetch messages: " + response.body());
                    }
                    return parseMessages(response.body());
                });
    }

    static List<Message> parseMessages(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode rows = JsonUtil.getMapper().readTree(json);
            if (!rows.isArray()) throw new IllegalStateException("The server returned an invalid message list.");
            for (com.fasterxml.jackson.databind.JsonNode row : rows) {
                // PostgREST embeds reverse relationships as arrays, even with an alias.
                // The desktop model displays one attachment and one poll per message.
                for (String field : List.of("attachment", "poll")) {
                    com.fasterxml.jackson.databind.JsonNode embedded = row.get(field);
                    if (embedded != null && embedded.isArray()) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) row).set(field,
                                embedded.isEmpty() ? com.fasterxml.jackson.databind.node.NullNode.instance : embedded.get(0));
                    }
                }
            }
            return JsonUtil.fromJsonList(rows.toString(), Message.class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read the message response.", e);
        }
    }

    public CompletableFuture<Message> sendMessage(Message message, String accessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", message.getId()); // UUID generated client-side for idempotent retries
        body.put("conversation_id", message.getConversationId());
        body.put("sender_id", message.getSenderId());
        body.put("message_type", message.getMessageType().name());
        body.put("content", message.getContent());
        if (message.getReplyToMessageId() != null) {
            body.put("reply_to_message_id", message.getReplyToMessageId());
        }

        String path = "/rest/v1/messages?select=*,sender_profile:profiles!messages_sender_id_fkey(*)";
        return HttpClientUtil.postAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to send message: " + response.body());
                    }
                    List<Message> list = parseMessages(response.body());
                    if (list.isEmpty()) throw new IllegalStateException("The server returned no message. Check the message read policy.");
                    return list.get(0);
                });
    }

    public CompletableFuture<Message> editMessage(String messageId, String newContent, String accessToken) {
        String path = "/rest/v1/messages?id=eq." + messageId + "&select=*";
        Map<String, Object> body = new HashMap<>();
        body.put("content", newContent);
        body.put("edited_at", java.time.Instant.now().toString());

        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to edit message: " + response.body());
                    }
                    List<Message> list = parseMessages(response.body());
                    if (list.isEmpty()) throw new IllegalStateException("The server returned no message. Check the message read policy.");
                    return list.get(0);
                });
    }

    public CompletableFuture<Void> softDeleteMessage(String messageId, String accessToken) {
        String path = "/rest/v1/messages?id=eq." + messageId;
        Map<String, Object> body = new HashMap<>();
        body.put("deleted_at", java.time.Instant.now().toString());
        body.put("content", "This message was deleted.");

        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> toggleReaction(String messageId, String userId, String emoji, boolean add, String accessToken) {
        if (add) {
            Map<String, Object> body = Map.of(
                    "message_id", messageId,
                    "user_id", userId,
                    "emoji", emoji
            );
            return HttpClientUtil.postAsync("/rest/v1/message_reactions", JsonUtil.toJson(body), accessToken)
                    .thenApply(r -> null);
        } else {
            String path = "/rest/v1/message_reactions?message_id=eq." + messageId + "&user_id=eq." + userId + "&emoji=eq." + emoji;
            return HttpClientUtil.deleteAsync(path, accessToken).thenApply(r -> null);
        }
    }

    public CompletableFuture<Void> markAsRead(String messageId, String userId, String accessToken) {
        Map<String, Object> body = Map.of(
                "message_id", messageId,
                "user_id", userId,
                "read_at", java.time.Instant.now().toString()
        );
        return HttpClientUtil.postAsync("/rest/v1/message_receipts?on_conflict=message_id,user_id", JsonUtil.toJson(body), accessToken)
                .thenApply(r -> null);
    }
}
