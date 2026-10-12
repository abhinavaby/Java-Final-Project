package com.connectai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String id;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("sender_id")
    private String senderId;
    
    @JsonProperty("message_type")
    private MessageType messageType = MessageType.TEXT;
    
    private String content;
    
    @JsonProperty("reply_to_message_id")
    private String replyToMessageId;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("edited_at")
    private String editedAt;
    
    @JsonProperty("deleted_at")
    private String deletedAt;

    // Transient & Helper fields
    private MessageStatus status = MessageStatus.SENT;
    @JsonProperty("sender_profile")
    private Profile senderProfile;
    private Message replyMessage;
    private Attachment attachment;
    private Poll poll;
    private List<MessageReaction> reactions = new ArrayList<>();
    private List<MessageReceipt> receipts = new ArrayList<>();
    private boolean isStarred;

    public Message() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getEditedAt() { return editedAt; }
    public void setEditedAt(String editedAt) { this.editedAt = editedAt; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Profile getSenderProfile() { return senderProfile; }
    public void setSenderProfile(Profile senderProfile) { this.senderProfile = senderProfile; }

    public Message getReplyMessage() { return replyMessage; }
    public void setReplyMessage(Message replyMessage) { this.replyMessage = replyMessage; }

    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public List<MessageReaction> getReactions() { return reactions; }
    public void setReactions(List<MessageReaction> reactions) { this.reactions = reactions; }

    public List<MessageReceipt> getReceipts() { return receipts; }
    public void setReceipts(List<MessageReceipt> receipts) { this.receipts = receipts; }

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public boolean isDeleted() {
        return deletedAt != null && !deletedAt.isBlank();
    }
}
