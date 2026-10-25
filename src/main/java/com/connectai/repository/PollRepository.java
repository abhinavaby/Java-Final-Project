package com.connectai.repository;

import com.connectai.model.Poll;
import com.connectai.model.PollOption;
import com.connectai.model.PollVote;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PollRepository {

    public CompletableFuture<Poll> createPoll(Poll poll, List<String> optionTexts, String accessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("message_id", poll.getMessageId());
        body.put("conversation_id", poll.getConversationId());
        body.put("created_by", poll.getCreatedBy());
        body.put("question", poll.getQuestion());
        body.put("allows_multiple_answers", poll.isAllowsMultipleAnswers());
        body.put("is_anonymous", poll.isAnonymous());
        if (poll.getClosesAt() != null) body.put("closes_at", poll.getClosesAt());

        return HttpClientUtil.postAsync("/rest/v1/polls?select=*", JsonUtil.toJson(body), accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to create poll: " + response.body());
                    }
                    List<Poll> list = JsonUtil.fromJsonList(response.body(), Poll.class);
                    Poll createdPoll = list.get(0);

                    // Create poll options
                    List<Map<String, Object>> optionsBody = new ArrayList<>();
                    for (int i = 0; i < optionTexts.size(); i++) {
                        optionsBody.add(Map.of(
                                "poll_id", createdPoll.getId(),
                                "option_text", optionTexts.get(i),
                                "position", i + 1
                        ));
                    }

                    return HttpClientUtil.postAsync("/rest/v1/poll_options?select=*", JsonUtil.toJson(optionsBody), accessToken)
                            .thenApply(optRes -> {
                                if (optRes.statusCode() >= 400) {
                                    throw new RuntimeException("Failed to create poll options: " + optRes.body());
                                }
                                List<PollOption> opts = JsonUtil.fromJsonList(optRes.body(), PollOption.class);
                                createdPoll.setOptions(opts);
                                return createdPoll;
                            });
                });
    }

    public CompletableFuture<Poll> getPollDetails(String pollId, String userId, String accessToken) {
        String path = "/rest/v1/polls?id=eq." + pollId + "&select=*,options:poll_options(*)";
        return HttpClientUtil.getAsync(path, accessToken)
                .thenCompose(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to load poll details");
                    }
                    List<Poll> list = JsonUtil.fromJsonList(response.body(), Poll.class);
                    if (list.isEmpty()) return CompletableFuture.completedFuture(null);
                    Poll poll = list.get(0);
                    return HttpClientUtil.getAsync("/rest/v1/rpc/get_poll_results?p_poll_id=" + pollId, accessToken)
                            .thenApply(resultsResponse -> {
                                if (resultsResponse.statusCode() >= 400)
                                    throw new RuntimeException("Could not load poll results. Apply database/poll-results.sql.");
                                List<com.connectai.model.PollOptionResult> results = JsonUtil.fromJsonList(
                                        resultsResponse.body(), com.connectai.model.PollOptionResult.class);
                                applyPollResults(poll, results);
                                return poll;
                            });
                });
    }

    public CompletableFuture<Void> castVote(String pollId, String optionId, String userId, boolean allowsMultipleAnswers, String accessToken) {
        if (!allowsMultipleAnswers) {
            // Delete previous vote for this user on single-choice poll
            String deletePath = "/rest/v1/poll_votes?poll_id=eq." + pollId + "&user_id=eq." + userId;
            return HttpClientUtil.deleteAsync(deletePath, accessToken)
                    .thenCompose(r -> {
                        if (r.statusCode() >= 400) throw new RuntimeException("Could not update your vote: " + r.body());
                        return insertVote(pollId, optionId, userId, accessToken);
                    });
        } else {
            return insertVote(pollId, optionId, userId, accessToken);
        }
    }

    public CompletableFuture<Void> retractVote(String pollId, String optionId, String userId, String accessToken) {
        String path = "/rest/v1/poll_votes?poll_id=eq." + pollId + "&option_id=eq." + optionId + "&user_id=eq." + userId;
        return HttpClientUtil.deleteAsync(path, accessToken).thenApply(r -> {
            if (r.statusCode() >= 400) throw new RuntimeException("Could not remove your vote: " + r.body());
            return null;
        });
    }

    private CompletableFuture<Void> insertVote(String pollId, String optionId, String userId, String accessToken) {
        Map<String, Object> body = Map.of(
                "poll_id", pollId,
                "option_id", optionId,
                "user_id", userId
        );
        return HttpClientUtil.postAsync("/rest/v1/poll_votes", JsonUtil.toJson(body), accessToken)
                .thenApply(r -> {
                    if (r.statusCode() >= 400) throw new RuntimeException("Could not save your vote: " + r.body());
                    return null;
                });
    }

    private void applyPollResults(Poll poll, List<com.connectai.model.PollOptionResult> results) {
        if (poll.getOptions() == null) return;
        int totalVotes = results == null || results.isEmpty() ? 0 : results.get(0).getTotalVotes();
        int totalVoters = results == null || results.isEmpty() ? 0 : results.get(0).getTotalVoters();
        poll.setTotalVotes(totalVotes);
        poll.setTotalVoters(totalVoters);
        List<String> selectedOptions = new ArrayList<>();
        if (results != null) for (var result : results) if (result.isHasVoted()) selectedOptions.add(result.getOptionId());
        poll.setUserVoted(!selectedOptions.isEmpty());
        poll.setUserVotedOptionIds(selectedOptions);
        for (PollOption option : poll.getOptions()) {
            int count = results == null ? 0 : results.stream()
                    .filter(result -> option.getId() != null && option.getId().equals(result.getOptionId()))
                    .mapToInt(com.connectai.model.PollOptionResult::getVoteCount).findFirst().orElse(0);
            option.setVoteCount(count);
            option.setHasVoted(selectedOptions.contains(option.getId()));
            option.setPercentage(totalVotes == 0 ? 0 : (count * 100.0 / totalVotes));
        }
    }
}
