package com.connectai.ui.chat;

import com.connectai.config.AppConfig;
import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeDimensions;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Conversation;
import com.connectai.model.Message;
import com.connectai.realtime.RealtimeEventListener;
import com.connectai.realtime.SupabaseRealtimeClient;
import com.connectai.service.AuthService;
import com.connectai.service.ChatService;
import com.connectai.ui.components.EmptyStatePanel;
import com.connectai.ui.components.RoundedPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class MainChatFrame extends JFrame implements RealtimeEventListener {
    private Conversation activeConversation;

    private ConversationListPanel leftSidebarPanel;
    private ChatHeaderPanel chatHeaderPanel;
    private JPanel messageListContainer;
    private JScrollPane messageScrollPane;
    private ComposerPanel composerPanel;
    private RightInfoPanel rightInfoPanel;

    private CardLayout centerCardLayout;
    private JPanel centerCardContainer;

    public MainChatFrame() {
        setTitle(AppConfig.getAppName() + " — AI-Powered Desktop Messenger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(ThemeDimensions.WINDOW_DEFAULT_SIZE);
        setMinimumSize(ThemeDimensions.WINDOW_MIN_SIZE);
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(ThemeColors.MAIN_BG);

        // 1. Left Navigation Sidebar
        leftSidebarPanel = new ConversationListPanel(this::openConversation);

        // 2. Center Chat Section
        chatHeaderPanel = new ChatHeaderPanel(this::toggleRightPanel);
        composerPanel = new ComposerPanel(this::reloadActiveMessages);

        messageListContainer = new JPanel();
        messageListContainer.setLayout(new BoxLayout(messageListContainer, BoxLayout.Y_AXIS));
        messageListContainer.setBackground(ThemeColors.MESSAGE_AREA);
        messageListContainer.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        messageScrollPane = new JScrollPane(messageListContainer);
        messageScrollPane.setBorder(null);
        messageScrollPane.setOpaque(false);
        messageScrollPane.getViewport().setOpaque(false);
        messageScrollPane.getViewport().setBackground(ThemeColors.MESSAGE_AREA);
        messageScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerCardLayout = new CardLayout();
        centerCardContainer = new JPanel(centerCardLayout);
        centerCardContainer.setOpaque(false);

        JPanel activeChatView = new JPanel(new BorderLayout());
        activeChatView.setBackground(ThemeColors.MESSAGE_AREA);
        activeChatView.add(chatHeaderPanel, BorderLayout.NORTH);
        activeChatView.add(messageScrollPane, BorderLayout.CENTER);
        activeChatView.add(composerPanel, BorderLayout.SOUTH);

        EmptyStatePanel emptyStateView = new EmptyStatePanel("💬", "No conversation selected",
                "Select a contact or group from the left panel to start messaging.");

        centerCardContainer.add(emptyStateView, "EMPTY");
        centerCardContainer.add(activeChatView, "CHAT");

        // 3. Right Info Panel (clean collapsible panel)
        rightInfoPanel = new RightInfoPanel(this::toggleRightPanel, () -> {
            openConversation(null);
            rightInfoPanel.setVisible(false);
            leftSidebarPanel.refreshConversations();
        });
        rightInfoPanel.setVisible(false);

        rootPanel.add(leftSidebarPanel, BorderLayout.WEST);
        rootPanel.add(centerCardContainer, BorderLayout.CENTER);
        rootPanel.add(rightInfoPanel, BorderLayout.EAST);

        setContentPane(rootPanel);
        String profileWarning = AuthService.getInstance().consumeProfileLoadWarning();
        if (profileWarning != null) SwingUtilities.invokeLater(() ->
                com.connectai.ui.components.ToastManager.showToast(this, profileWarning,
                        com.connectai.ui.components.ToastManager.ToastType.WARNING));

        // Connect Realtime client
        SupabaseRealtimeClient.getInstance().addListener(this);
        SupabaseRealtimeClient.getInstance().connect();

        centerCardLayout.show(centerCardContainer, "EMPTY");
    }

    private void openConversation(Conversation conv) {
        this.activeConversation = conv;
        if (conv == null) {
            centerCardLayout.show(centerCardContainer, "EMPTY");
            return;
        }

        chatHeaderPanel.setConversation(conv);
        composerPanel.setConversationId(conv.getId());
        rightInfoPanel.setConversation(conv);
        centerCardLayout.show(centerCardContainer, "CHAT");

        reloadActiveMessages();
    }

    private void reloadActiveMessages() {
        if (activeConversation == null) return;

        String requestedConversationId = activeConversation.getId();
        new SwingWorker<List<Message>, Void>() {
            @Override
            protected List<Message> doInBackground() throws Exception {
                return ChatService.getInstance().loadMessages(requestedConversationId, 50, 0).get();
            }

            @Override
            protected void done() {
                try {
                    List<Message> msgs = get();
                    if (activeConversation == null || !requestedConversationId.equals(activeConversation.getId())) return;
                    messageListContainer.removeAll();

                    // Centered "Today" Date Badge pill
                    messageListContainer.add(Box.createVerticalStrut(10));
                    messageListContainer.add(createDateSeparatorPill("Today"));
                    messageListContainer.add(Box.createVerticalStrut(14));

                    String currentUserId = AuthService.getInstance().getCurrentUser() != null
                            ? AuthService.getInstance().getCurrentUser().getId() : "";

                    // Messages come ordered desc from DB, reverse to render chronologically
                    for (int i = msgs.size() - 1; i >= 0; i--) {
                        Message m = msgs.get(i);
                        boolean isOutgoing = currentUserId.equals(m.getSenderId());
                        MessageBubble bubble = new MessageBubble(
                                m, isOutgoing,
                                replyMsg -> composerPanel.setReplyToMessage(replyMsg),
                                editMsg -> composerPanel.setEditTargetMessage(editMsg),
                                deleteMsg -> ChatService.getInstance().deleteMessage(deleteMsg.getId()).thenRun(() -> reloadActiveMessages()),
                                emoji -> ChatService.getInstance().toggleReaction(m.getId(), emoji, !hasCurrentUserReaction(m, emoji))
                                        .thenRun(() -> MainChatFrame.this.reloadActiveMessages())
                                        .exceptionally(error -> {
                                            SwingUtilities.invokeLater(() -> com.connectai.ui.components.ToastManager.showToast(
                                                    MainChatFrame.this, com.connectai.util.ErrorMessages.from(error),
                                                    com.connectai.ui.components.ToastManager.ToastType.ERROR));
                                            return null;
                                        })
                        );
                        messageListContainer.add(bubble);
                        messageListContainer.add(Box.createVerticalStrut(8));
                    }

                    messageListContainer.revalidate();
                    messageListContainer.repaint();
                    scrollToBottom();
                } catch (Exception error) {
                    if (activeConversation != null && requestedConversationId.equals(activeConversation.getId()))
                        com.connectai.ui.components.ToastManager.showToast(MainChatFrame.this,
                                "Could not display messages: " + com.connectai.util.ErrorMessages.from(error),
                                com.connectai.ui.components.ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }

    private boolean hasCurrentUserReaction(Message message, String emoji) {
        String userId = AuthService.getInstance().getCurrentUser() == null
                ? "" : AuthService.getInstance().getCurrentUser().getId();
        return message.getReactions() != null && message.getReactions().stream()
                .anyMatch(reaction -> emoji.equals(reaction.getEmoji()) && userId.equals(reaction.getUserId()));
    }

    private JPanel createDateSeparatorPill(String dateText) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);

        JPanel pill = new RoundedPanel(12, ThemeColors.DATE_PILL_BG);
        pill.setLayout(new FlowLayout(FlowLayout.CENTER, 14, 4));
        pill.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel l = new JLabel(dateText);
        l.setFont(ThemeFonts.BADGE);
        l.setForeground(ThemeColors.SECONDARY_TEXT);

        pill.add(l);
        wrapper.add(pill);
        return wrapper;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            var bar = messageScrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void toggleRightPanel() {
        rightInfoPanel.setVisible(!rightInfoPanel.isVisible());
        revalidate();
        repaint();
    }

    @Override
    public void onNewMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (activeConversation != null && message.getConversationId() != null
                    && message.getConversationId().equals(activeConversation.getId())) reloadActiveMessages();
            leftSidebarPanel.refreshConversations();
        });
    }

    @Override
    public void onConnectionStateChanged(boolean isConnected) {
        // Status handled
    }

    @Override
    public void onMessageEdited(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (activeConversation != null && message != null && message.getConversationId() != null
                    && message.getConversationId().equals(activeConversation.getId())) reloadActiveMessages();
        });
    }

    @Override
    public void onReactionUpdated(com.connectai.model.MessageReaction reaction) {
        SwingUtilities.invokeLater(() -> {
            if (activeConversation != null) reloadActiveMessages();
        });
    }

    @Override
    public void onPollVoted(com.connectai.model.PollVote vote) {
        SwingUtilities.invokeLater(() -> {
            if (vote == null || activeConversation == null) return;
            reloadActiveMessages();
        });
    }
}
