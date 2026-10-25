package com.connectai.service;

import com.connectai.model.Conversation;
import com.connectai.model.ConversationMember;
import com.connectai.model.Message;
import com.connectai.model.MessageStatus;
import com.connectai.model.MessageType;
import com.connectai.model.Role;
import com.connectai.repository.ConversationRepository;
import com.connectai.repository.MessageRepository;
import com.connectai.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChatService {
    private static final ChatService instance = new ChatService();

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final com.connectai.repository.StorageRepository storageRepository = new com.connectai.repository.StorageRepository();

    private ChatService() {
        this.conversationRepository = new ConversationRepository();
        this.messageRepository = new MessageRepository();
        this.profileRepository = new ProfileRepository();
    }

    public static ChatService getInstance() {
        return instance;
    }

    public CompletableFuture<List<ConversationMember>> loadUserConversations() {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().getUserMemberships());
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return conversationRepository.getUserMemberships(userId, token);
    }

    public CompletableFuture<Conversation> startDirectChat(String partnerUserId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().startDirectConversation(partnerUserId));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return conversationRepository.createDirectConversation(userId, partnerUserId, token)
                .thenCompose(conversation -> profileRepository.getProfileById(partnerUserId, token)
                        .thenApply(profile -> {
                            conversation.setDirectPartnerProfile(profile);
                            return conversation;
                        }));
    }

    public CompletableFuture<List<com.connectai.model.Profile>> searchProfilesByUsername(String usernameQuery) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().searchProfiles(usernameQuery));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return profileRepository.searchProfiles(usernameQuery, userId, token);
    }

    public CompletableFuture<com.connectai.model.Profile> updateCurrentProfile(String displayName, String about, String avatarPath) {
        var signedIn = AuthService.getInstance().getCurrentProfile();
        if (signedIn == null) return CompletableFuture.failedFuture(new IllegalStateException("No signed-in profile."));
        if (com.connectai.config.AppConfig.isMockMode()) {
            var updated = com.connectai.mock.MockDataProvider.getInstance().updateCurrentProfile(displayName.trim(), about.trim());
            if (avatarPath != null) updated.setAvatarPath(avatarPath);
            return CompletableFuture.completedFuture(updated);
        }
        var candidate = new com.connectai.model.Profile(signedIn.getId(), signedIn.getUsername(), displayName.trim(),
                avatarPath == null ? signedIn.getAvatarPath() : avatarPath);
        candidate.setAbout(about.trim());
        return profileRepository.updateProfile(candidate, AuthService.getInstance().getAccessToken()).thenApply(updated -> {
            signedIn.setDisplayName(updated.getDisplayName());
            signedIn.setAbout(updated.getAbout());
            signedIn.setAvatarPath(updated.getAvatarPath());
            return signedIn;
        });
    }

    public CompletableFuture<String> uploadProfilePicture(java.io.File imageFile) {
        if (com.connectai.config.AppConfig.isMockMode()) return CompletableFuture.completedFuture(null);
        try {
            String userId = AuthService.getInstance().getCurrentUser().getId();
            String extension = imageFile.getName().contains(".")
                    ? imageFile.getName().substring(imageFile.getName().lastIndexOf('.') + 1).toLowerCase() : "png";
            if (!java.util.Set.of("png", "jpg", "jpeg", "webp").contains(extension))
                return CompletableFuture.failedFuture(new IllegalArgumentException("Choose a PNG, JPG, or WEBP image."));
            String mime = java.util.Objects.requireNonNullElse(java.nio.file.Files.probeContentType(imageFile.toPath()), "image/png");
            String path = userId + "/" + UUID.randomUUID() + "." + extension;
            return storageRepository.uploadAvatar(path, java.nio.file.Files.readAllBytes(imageFile.toPath()), mime,
                    AuthService.getInstance().getAccessToken());
        } catch (Exception e) { return CompletableFuture.failedFuture(e); }
    }

    public CompletableFuture<Conversation> createGroup(String name, String description, boolean isPrivate) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().createGroup(name, description, isPrivate));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return conversationRepository.createGroupConversation(name, description, userId, isPrivate, token);
    }

    public CompletableFuture<Void> deleteGroup(Conversation group) {
        if (group.getType() != com.connectai.model.ConversationType.GROUP ||
                !AuthService.getInstance().getCurrentUser().getId().equals(group.getCreatedBy()))
            return CompletableFuture.failedFuture(new IllegalStateException("Only the group creator can delete it."));
        return conversationRepository.deleteGroup(group.getId(), AuthService.getInstance().getAccessToken());
    }

    public CompletableFuture<Conversation> joinGroupByCode(String inviteCode) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().joinGroupByInviteCode(inviteCode));
        }
        String token = AuthService.getInstance().getAccessToken();
        return conversationRepository.joinGroupByInviteCode(inviteCode, token);
    }

    public CompletableFuture<List<Message>> loadMessages(String conversationId, int limit, int offset) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().getMessages(conversationId));
        }
        String token = AuthService.getInstance().getAccessToken();
        return messageRepository.getMessages(conversationId, limit, offset, token);
    }

    public CompletableFuture<Message> sendTextMessage(String conversationId, String text, String replyToId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().sendMessage(conversationId, text, replyToId));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();

        Message msg = new Message();
        msg.setId(UUID.randomUUID().toString()); // Optimistic UUID creation
        msg.setConversationId(conversationId);
        msg.setSenderId(userId);
        msg.setMessageType(MessageType.TEXT);
        msg.setContent(text);
        msg.setReplyToMessageId(replyToId);
        msg.setStatus(MessageStatus.SENDING);
        msg.setCreatedAt(java.time.Instant.now().toString());

        return messageRepository.sendMessage(msg, token)
                .thenApply(sentMsg -> {
                    sentMsg.setStatus(MessageStatus.SENT);
                    return sentMsg;
                });
    }

    public CompletableFuture<Message> editMessage(String messageId, String newText) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().editMessage(messageId, newText));
        }
        String token = AuthService.getInstance().getAccessToken();
        return messageRepository.editMessage(messageId, newText, token);
    }

    public CompletableFuture<Void> deleteMessage(String messageId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            com.connectai.mock.MockDataProvider.getInstance().deleteMessage(messageId);
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        return messageRepository.softDeleteMessage(messageId, token);
    }

    public CompletableFuture<Void> toggleReaction(String messageId, String emoji, boolean add) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            com.connectai.mock.MockDataProvider.getInstance().toggleReaction(messageId, emoji);
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return messageRepository.toggleReaction(messageId, userId, emoji, add, token);
    }

    public CompletableFuture<List<ConversationMember>> getGroupMembers(String conversationId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().getConversationMembers(conversationId));
        }
        String token = AuthService.getInstance().getAccessToken();
        return conversationRepository.getConversationMembers(conversationId, token);
    }

    public CompletableFuture<Void> updateMemberRole(String conversationId, String memberUserId, Role newRole) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        return conversationRepository.updateMemberRole(conversationId, memberUserId, newRole, token);
    }

    public CompletableFuture<Void> removeMember(String conversationId, String memberUserId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        return conversationRepository.removeMember(conversationId, memberUserId, token);
    }

    public CompletableFuture<Void> leaveGroup(String conversationId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            com.connectai.mock.MockDataProvider.getInstance().leaveGroup(conversationId);
            return CompletableFuture.completedFuture(null);
        }
        return conversationRepository.leaveGroup(conversationId, AuthService.getInstance().getAccessToken());
    }
}
