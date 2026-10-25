package com.connectai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Poll {
    private String id;
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("created_by")
    private String createdBy;
    
    private String question;
    
    @JsonProperty("allows_multiple_answers")
    private boolean allowsMultipleAnswers;
    
    @JsonProperty("is_anonymous")
    private boolean isAnonymous;
    
    @JsonProperty("closes_at")
    private String closesAt;
    
    @JsonProperty("created_at")
    private String createdAt;

    private List<PollOption> options = new ArrayList<>();
    private List<PollVote> votes = new ArrayList<>();
    private int totalVoters;
    private int totalVotes;
    private boolean userVoted;
    private List<String> userVotedOptionIds = new ArrayList<>();

    public Poll() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public boolean isAllowsMultipleAnswers() { return allowsMultipleAnswers; }
    public void setAllowsMultipleAnswers(boolean allowsMultipleAnswers) { this.allowsMultipleAnswers = allowsMultipleAnswers; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public String getClosesAt() { return closesAt; }
    public void setClosesAt(String closesAt) { this.closesAt = closesAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<PollOption> getOptions() { return options; }
    public void setOptions(List<PollOption> options) { this.options = options; }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public List<PollVote> getVotes() { return votes; }
    public void setVotes(List<PollVote> votes) { this.votes = votes; }

    public int getTotalVoters() { return totalVoters; }
    public void setTotalVoters(int totalVoters) { this.totalVoters = totalVoters; }

    public int getTotalVotes() { return totalVotes; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }

    public boolean isUserVoted() { return userVoted; }
    public void setUserVoted(boolean userVoted) { this.userVoted = userVoted; }

    public List<String> getUserVotedOptionIds() { return userVotedOptionIds; }
    public void setUserVotedOptionIds(List<String> userVotedOptionIds) { this.userVotedOptionIds = userVotedOptionIds; }
}
