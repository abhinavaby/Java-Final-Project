package com.connectai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PollOptionResult {
    @JsonProperty("option_id") private String optionId;
    @JsonProperty("vote_count") private int voteCount;
    @JsonProperty("total_voters") private int totalVoters;
    @JsonProperty("total_votes") private int totalVotes;
    @JsonProperty("has_voted") private boolean hasVoted;

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    public int getTotalVoters() { return totalVoters; }
    public void setTotalVoters(int totalVoters) { this.totalVoters = totalVoters; }
    public int getTotalVotes() { return totalVotes; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }
    public boolean isHasVoted() { return hasVoted; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }
}
