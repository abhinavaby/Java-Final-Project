package com.connectai.ui.poll;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Poll;
import com.connectai.model.PollOption;
import com.connectai.service.PollService;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class PollCardComponent extends JPanel {
    private Poll poll;

    public PollCardComponent(Poll poll) {
        this.poll = poll;
        setLayout(new BorderLayout());
        setOpaque(false);
        renderPoll();
        refreshPollDetails();
    }

    private void renderPoll() {
        removeAll();
        if (poll == null)
            return;

        JPanel card = new RoundedPanel(12, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header
        JLabel questionLabel = new JLabel("📊 " + poll.getQuestion());
        questionLabel.setFont(ThemeFonts.BODY_BOLD);
        questionLabel.setForeground(ThemeColors.PRIMARY_TEXT);
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String subText = (poll.isAllowsMultipleAnswers() ? "Multiple choice" : "Single choice") +
                (poll.isAnonymous() ? " • Anonymous" : "");
        JLabel typeLabel = new JLabel(subText);
        typeLabel.setFont(ThemeFonts.CAPTION);
        typeLabel.setForeground(ThemeColors.MUTED_TEXT);
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(questionLabel);
        card.add(typeLabel);
        card.add(javax.swing.Box.createVerticalStrut(10));

        // Options
        if (poll.getOptions() != null) {
            for (PollOption opt : poll.getOptions()) {
                boolean hasVotedThis = poll.getUserVotedOptionIds() != null
                        && poll.getUserVotedOptionIds().contains(opt.getId());
                JPanel optBar = createOptionBar(opt, hasVotedThis);
                card.add(optBar);
                card.add(javax.swing.Box.createVerticalStrut(6));
            }
        }

        // Footer
        JLabel footerLabel = new JLabel(poll.getTotalVoters() + " voters · " + poll.getTotalVotes() + " votes");
        footerLabel.setFont(ThemeFonts.BODY_SMALL);
        footerLabel.setForeground(ThemeColors.MUTED_TEXT);

        card.add(javax.swing.Box.createVerticalStrut(6));
        card.add(footerLabel);

        add(card, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createOptionBar(PollOption option, boolean selected) {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Progress percentage fill background
                if (option.getPercentage() > 0) {
                    int fillWidth = (int) ((getWidth() * option.getPercentage()) / 100.0);
                    g2.setColor(selected ? new Color(37, 99, 235, 35) : new Color(226, 232, 240, 180));
                    g2.fillRoundRect(0, 0, fillWidth, getHeight(), 8, 8);
                }

                g2.setColor(selected ? ThemeColors.PRIMARY_ACCENT : ThemeColors.CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };

        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(320, 36));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JLabel text = new JLabel((selected ? "✓ " : "") + option.getOptionText());
        text.setFont(ThemeFonts.BODY_MEDIUM);
        text.setForeground(selected ? ThemeColors.PRIMARY_ACCENT : ThemeColors.PRIMARY_TEXT);

        JLabel pct = new JLabel(String.format("%.0f%%", option.getPercentage()));
        pct.setFont(ThemeFonts.BODY_BOLD);
        pct.setForeground(ThemeColors.SECONDARY_TEXT);

        bar.add(text, BorderLayout.WEST);
        bar.add(pct, BorderLayout.EAST);

        bar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        bar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                castVote(option.getId(), selected);
            }
        });

        return bar;
    }

    private void castVote(String optionId, boolean alreadySelected) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                PollService.getInstance().toggleVote(poll.getId(), optionId, poll.isAllowsMultipleAnswers(), alreadySelected).get();
                poll = PollService.getInstance().refreshPoll(poll.getId()).get();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    renderPoll();
                } catch (Exception error) {
                    com.connectai.ui.components.ToastManager.showToast(PollCardComponent.this,
                            com.connectai.util.ErrorMessages.from(error),
                            com.connectai.ui.components.ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }

    private void refreshPollDetails() {
        if (poll == null || poll.getId() == null || poll.getId().isBlank()) return;
        PollService.getInstance().refreshPoll(poll.getId()).whenComplete((updated, error) ->
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (error == null && updated != null) {
                        poll = updated;
                        renderPoll();
                    }
                }));
    }
}
