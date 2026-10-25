package com.connectai.service;

import com.connectai.model.Message;
import com.connectai.model.MessageStatus;
import com.connectai.model.MessageType;
import com.connectai.model.Poll;
import com.connectai.repository.MessageRepository;
import com.connectai.repository.PollRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PollService {
    private static final PollService instance = new PollService();

    private final PollRepository pollRepository;
    private final MessageRepository messageRepository;

    private PollService() {
        this.pollRepository = new PollRepository();
        this.messageRepository = new MessageRepository();
    }

    public static PollService getInstance() {
        return instance;
    }

    public CompletableFuture<Message> createPollMessage(String conversationId, String question, List<String> options, boolean allowsMultiple, boolean isAnonymous) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().createPoll(conversationId, question, options, allowsMultiple, isAnonymous));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();

        String messageId = UUID.randomUUID().toString();

        Message msg = new Message();
        msg.setId(messageId);
        msg.setConversationId(conversationId);
        msg.setSenderId(userId);
        msg.setMessageType(MessageType.POLL);
        msg.setContent("Poll: " + question);
        msg.setStatus(MessageStatus.SENDING);
        msg.setCreatedAt(java.time.Instant.now().toString());

        return messageRepository.sendMessage(msg, token)
                .thenCompose(sentMsg -> {
                    Poll poll = new Poll();
                    poll.setMessageId(sentMsg.getId());
                    poll.setConversationId(conversationId);
                    poll.setCreatedBy(userId);
                    poll.setQuestion(question);
                    poll.setAllowsMultipleAnswers(allowsMultiple);
                    poll.setAnonymous(isAnonymous);

                    return pollRepository.createPoll(poll, options, token)
                            .thenApply(createdPoll -> {
                                sentMsg.setPoll(createdPoll);
                                sentMsg.setStatus(MessageStatus.SENT);
                                return sentMsg;
                            });
                });
    }

    public CompletableFuture<Void> vote(String pollId, String optionId, boolean allowsMultiple) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            com.connectai.mock.MockDataProvider.getInstance().votePoll(pollId, optionId, allowsMultiple);
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return pollRepository.castVote(pollId, optionId, userId, allowsMultiple, token);
    }

    public CompletableFuture<Void> toggleVote(String pollId, String optionId, boolean allowsMultiple, boolean alreadySelected) {
        if (!alreadySelected) return vote(pollId, optionId, allowsMultiple);
        if (!allowsMultiple) return CompletableFuture.completedFuture(null);
        if (com.connectai.config.AppConfig.isMockMode()) {
            com.connectai.mock.MockDataProvider.getInstance().votePoll(pollId, optionId, true);
            return CompletableFuture.completedFuture(null);
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return pollRepository.retractVote(pollId, optionId, userId, token);
    }

    public CompletableFuture<Poll> refreshPoll(String pollId) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return CompletableFuture.completedFuture(com.connectai.mock.MockDataProvider.getInstance().getPoll(pollId));
        }
        String token = AuthService.getInstance().getAccessToken();
        String userId = AuthService.getInstance().getCurrentUser().getId();
        return pollRepository.getPollDetails(pollId, userId, token);
    }
}
