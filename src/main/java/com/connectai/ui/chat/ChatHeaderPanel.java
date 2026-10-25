package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Conversation;
import com.connectai.model.ConversationType;
import com.connectai.service.AIService;
import com.connectai.ui.components.AISummaryDialog;
import com.connectai.ui.components.AvatarComponent;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.ToastManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Clean and uncluttered chat header panel.
 */
public class ChatHeaderPanel extends JPanel {
    private Conversation conversation;
    private AvatarComponent avatar;
    private JLabel titleLabel;
    private JLabel subTitleLabel;
    private Runnable onToggleRightPanel;

    public ChatHeaderPanel(Runnable onToggleRightPanel) {
        this.onToggleRightPanel = onToggleRightPanel;
        setLayout(new BorderLayout(14, 0));
        setBackground(ThemeColors.MESSAGE_AREA);
        setPreferredSize(new Dimension(600, 58));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.DIVIDER),
                BorderFactory.createEmptyBorder(8, 18, 8, 16)
        ));

        // Left section: Avatar + Title + Status
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);

        avatar = new AvatarComponent(40, "ConnectAI Assistant 🤖", true);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        titleLabel = new JLabel("Tech & AI Product Team");
        titleLabel.setFont(ThemeFonts.TITLE_SMALL);
        titleLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        subTitleLabel = new JLabel("Group Conversation • 6 members");
        subTitleLabel.setFont(ThemeFonts.CAPTION);
        subTitleLabel.setForeground(ThemeColors.MUTED_TEXT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subTitleLabel);

        leftGroup.add(avatar);
        leftGroup.add(textPanel);

        // Right actions: ✨ Catch Me Up, ℹ Details
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actions.setOpaque(false);

        // Pill Button: ✨ Catch Me Up
        JPanel catchMeUpPill = new RoundedPanel(14, Color.decode("#EFF6FF"), Color.decode("#BFDBFE"));
        catchMeUpPill.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        catchMeUpPill.setPreferredSize(new Dimension(126, 30));
        catchMeUpPill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel sparkleIcon = new JLabel("✨");
        sparkleIcon.setFont(ThemeFonts.BODY_SMALL);
        sparkleIcon.setForeground(ThemeColors.SPARKLE_GOLD);

        JLabel catchText = new JLabel("Catch Me Up");
        catchText.setFont(ThemeFonts.BODY_BOLD);
        catchText.setForeground(ThemeColors.PRIMARY_ACCENT);

        catchMeUpPill.add(sparkleIcon);
        catchMeUpPill.add(catchText);

        catchMeUpPill.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                triggerCatchMeUp();
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                catchMeUpPill.setBackground(Color.decode("#DBEAFE"));
                catchMeUpPill.repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                catchMeUpPill.setBackground(Color.decode("#EFF6FF"));
                catchMeUpPill.repaint();
            }
        });

        IconButton infoBtn = new IconButton("ℹ", "Conversation Details");
        infoBtn.setPreferredSize(new Dimension(30, 30));
        infoBtn.addActionListener(e -> {
            if (onToggleRightPanel != null) onToggleRightPanel.run();
        });

        actions.add(catchMeUpPill);
        actions.add(infoBtn);

        add(leftGroup, BorderLayout.WEST);
        add(actions, BorderLayout.EAST);
    }

    public void setConversation(Conversation conv) {
        this.conversation = conv;
        if (conv == null) {
            titleLabel.setText("ConnectAI Assistant 🤖");
            subTitleLabel.setText("● Online");
            return;
        }

        avatar.setDisplayName(conv.getDisplayTitle());
        titleLabel.setText(conv.getDisplayTitle());

        if (conv.getType() == ConversationType.GROUP) {
            int count = conv.getMembers() != null ? conv.getMembers().size() : 6;
            subTitleLabel.setText("Group Conversation • " + count + " members");
        } else {
            subTitleLabel.setText("● Online");
            subTitleLabel.setForeground(ThemeColors.SUCCESS_ONLINE);
        }
    }

    private void triggerCatchMeUp() {
        if (conversation == null) {
            ToastManager.showToast(this, "Select a conversation to summarize", ToastManager.ToastType.WARNING);
            return;
        }

        if (!com.connectai.config.AppConfig.isMockMode()
                && javax.swing.JOptionPane.showConfirmDialog(this,
                        "Recent messages from this conversation will be sent to the configured AI service through Supabase to create a summary. Continue?",
                        "Send conversation to AI", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                        javax.swing.JOptionPane.INFORMATION_MESSAGE) != javax.swing.JOptionPane.OK_OPTION) return;

        Frame top = (Frame) SwingUtilities.getWindowAncestor(this);
        ToastManager.showToast(this, "Generating AI summary...", ToastManager.ToastType.INFO);

        new SwingWorker<com.connectai.model.AISummaryResponse, Void>() {
            @Override
            protected com.connectai.model.AISummaryResponse doInBackground() throws Exception {
                return AIService.getInstance().summarizeConversation(conversation.getId()).get();
            }

            @Override
            protected void done() {
                try {
                    com.connectai.model.AISummaryResponse res = get();
                    new AISummaryDialog(top, res).setVisible(true);
                } catch (Exception ex) {
                    ToastManager.showToast(ChatHeaderPanel.this, "Summary failed: " + com.connectai.util.ErrorMessages.from(ex),
                            ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }
}
