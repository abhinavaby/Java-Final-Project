package com.connectai.repository;

import com.connectai.model.Conversation;
import com.connectai.model.ConversationMember;
import com.connectai.model.ConversationType;
import com.connectai.model.Role;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConversationRepository {

    public CompletableFuture<List<ConversationMember>> getUserMemberships(String userId, String accessToken) {
        String path = "/rest/v1/conversation_members?user_id=eq." + userId + "&select=*,conversation:conversations(*)";
        return HttpClientUtil.getAsync(path, accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to load user conversations: " + response.body());
                    }
                    List<ConversationMember> list = JsonUtil.fromJsonList(response.body(), ConversationMember.class);
                    if (list != null && !list.isEmpty()) {
                        return CompletableFuture.completedFuture(list);
                    }

                    // Auto-enroll in default public channels
                    String pubPath = "/rest/v1/conversations?is_private=eq.false&select=*";
                    return HttpClientUtil.getAsync(pubPath, accessToken)
                            .thenApply(pubRes -> {
                                List<ConversationMember> defaultList = new java.util.ArrayList<>();
                                if (pubRes.statusCode() < 400) {
                                    List<Conversation> pubConvs = JsonUtil.fromJsonList(pubRes.body(), Conversation.class);
                                    for (Conversation pc : pubConvs) {
                                        // Auto-insert membership into Supabase in background
                                        Map<String, Object> joinMap = Map.of(
                                                "conversation_id", pc.getId(),
                                                "user_id", userId,
                                                "role", Role.MEMBER.name()
                                        );
                                        HttpClientUtil.postAsync("/rest/v1/conversation_members", JsonUtil.toJson(joinMap), accessToken);

                                        ConversationMember cm = new ConversationMember();
                                        cm.setConversationId(pc.getId());
                                        cm.setUserId(userId);
                                        cm.setRole(Role.MEMBER);
                                        cm.setConversation(pc);
                                        defaultList.add(cm);
                                    }
                                }
                                return defaultList;
                            });
                });
    }

    public CompletableFuture<Conversation> createDirectConversation(String currentUserId, String partnerUserId, String accessToken) {
        // First check if a direct conversation already exists
        String checkPath = "/rest/v1/conversations?type=eq.DIRECT&select=*,conversation_members(*)";
        return HttpClientUtil.getAsync(checkPath, accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() < 400) {
                        List<Conversation> existingList = JsonUtil.fromJsonList(response.body(), Conversation.class);
                        for (Conversation conv : existingList) {
                            if (conv.getMembers() != null) {
                                boolean hasCurrent = conv.getMembers().stream().anyMatch(m -> m.getUserId().equals(currentUserId));
                                boolean hasPartner = conv.getMembers().stream().anyMatch(m -> m.getUserId().equals(partnerUserId));
                                if (hasCurrent && hasPartner) {
                                    return CompletableFuture.completedFuture(conv);
                                }
                            }
                        }
                    }

                    // Create new direct conversation
                    Map<String, Object> body = new HashMap<>();
                    body.put("type", ConversationType.DIRECT.name());
                    body.put("created_by", currentUserId);

                    return HttpClientUtil.postAsync("/rest/v1/conversations?select=*", JsonUtil.toJson(body), accessToken)
                            .thenCompose(createRes -> {
                                if (createRes.statusCode() >= 400) {
                                    throw new RuntimeException("Failed to create direct conversation: " + createRes.body());
                                }
                                List<Conversation> createdList = JsonUtil.fromJsonList(createRes.body(), Conversation.class);
                                if (createdList.isEmpty()) throw new IllegalStateException("The database returned no conversation. Check the conversation read policies.");
                    Conversation conv = createdList.get(0);

                                // Add both users as members
                                Map<String, Object> m1 = Map.of("conversation_id", conv.getId(), "user_id", currentUserId, "role", Role.OWNER.name());
                                Map<String, Object> m2 = Map.of("conversation_id", conv.getId(), "user_id", partnerUserId, "role", Role.MEMBER.name());
                                List<Map<String, Object>> membersBody = List.of(m1, m2);

                                return HttpClientUtil.postAsync("/rest/v1/conversation_members", JsonUtil.toJson(membersBody), accessToken)
                                        .thenApply(mRes -> {
                                            if (mRes.statusCode() >= 400) throw new RuntimeException("Could not add conversation members: " + mRes.body());
                                            return conv;
                                        });
                            });
                });
    }

    public CompletableFuture<Conversation> createGroupConversation(String name, String description, String createdByUserId, boolean isPrivate, String accessToken) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> body = new HashMap<>();
        body.put("type", ConversationType.GROUP.name());
        body.put("name", name);
        body.put("description", description);
        body.put("created_by", createdByUserId);
        body.put("invite_code", inviteCode);
        body.put("is_private", isPrivate);

        return HttpClientUtil.postAsync("/rest/v1/conversations?select=*", JsonUtil.toJson(body), accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to create group: " + response.body());
                    }
                    List<Conversation> createdList = JsonUtil.fromJsonList(response.body(), Conversation.class);
                    if (createdList.isEmpty()) throw new IllegalStateException("The database returned no conversation. Check the conversation read policies.");
                    Conversation conv = createdList.get(0);

                    // Add creator as OWNER
                    Map<String, Object> memberBody = Map.of(
                            "conversation_id", conv.getId(),
                            "user_id", createdByUserId,
                            "role", Role.OWNER.name()
                    );
                    return HttpClientUtil.postAsync("/rest/v1/conversation_members", JsonUtil.toJson(memberBody), accessToken)
                            .thenApply(r -> {
                                if (r.statusCode() >= 400) throw new RuntimeException("Could not join group: " + r.body());
                                return conv;
                            });
                });
    }

    public CompletableFuture<Void> deleteGroup(String id, String accessToken) {
        return HttpClientUtil.deleteAsync("/rest/v1/conversations?id=eq." + id + "&select=id", accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) throw new IllegalStateException("Could not delete group: " + response.body());
                    if (response.body().isBlank() || response.body().trim().equals("[]"))
                        throw new IllegalStateException("Group was not deleted. Only its creator can delete it; check the delete policy.");
                    return null;
                });
    }

    public CompletableFuture<Conversation> joinGroupByInviteCode(String inviteCode, String userId, String accessToken) {
        String path = "/rest/v1/conversations?invite_code=eq." + inviteCode.trim().toUpperCase() + "&select=*";
        return HttpClientUtil.getAsync(path, accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Invalid invite code");
                    }
                    List<Conversation> list = JsonUtil.fromJsonList(response.body(), Conversation.class);
                    if (list.isEmpty()) {
                        throw new RuntimeException("No group found with invite code: " + inviteCode);
                    }
                    Conversation conv = list.get(0);

                    Map<String, Object> memberBody = Map.of(
                            "conversation_id", conv.getId(),
                            "user_id", userId,
                            "role", Role.MEMBER.name()
                    );

                    return HttpClientUtil.postAsync("/rest/v1/conversation_members", JsonUtil.toJson(memberBody), accessToken)
                            .thenApply(r -> {
                                if (r.statusCode() >= 400) throw new RuntimeException("Could not join group: " + r.body());
                                return conv;
                            });
                });
    }

    public CompletableFuture<List<ConversationMember>> getConversationMembers(String conversationId, String accessToken) {
        String path = "/rest/v1/conversation_members?conversation_id=eq." + conversationId + "&select=*,profile:profiles(*)";
        return HttpClientUtil.getAsync(path, accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to load group members");
                    }
                    return JsonUtil.fromJsonList(response.body(), ConversationMember.class);
                });
    }

    public CompletableFuture<Void> updateMemberRole(String conversationId, String userId, Role newRole, String accessToken) {
        String path = "/rest/v1/conversation_members?conversation_id=eq." + conversationId + "&user_id=eq." + userId;
        Map<String, Object> body = Map.of("role", newRole.name());
        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> removeMember(String conversationId, String userId, String accessToken) {
        String path = "/rest/v1/conversation_members?conversation_id=eq." + conversationId + "&user_id=eq." + userId;
        return HttpClientUtil.deleteAsync(path, accessToken)
                .thenApply(r -> null);
    }

    public CompletableFuture<Void> togglePin(String conversationId, String userId, boolean isPinned, String accessToken) {
        String path = "/rest/v1/conversation_members?conversation_id=eq." + conversationId + "&user_id=eq." + userId;
        Map<String, Object> body = Map.of("is_pinned", isPinned);
        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken).thenApply(r -> null);
    }

    public CompletableFuture<Void> toggleMute(String conversationId, String userId, boolean isMuted, String accessToken) {
        String path = "/rest/v1/conversation_members?conversation_id=eq." + conversationId + "&user_id=eq." + userId;
        Map<String, Object> body = Map.of("is_muted", isMuted);
        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken).thenApply(r -> null);
    }
}
