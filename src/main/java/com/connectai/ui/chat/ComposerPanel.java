package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Message;
import com.connectai.service.AttachmentService;
import com.connectai.service.AudioService;
import com.connectai.service.ChatService;
import com.connectai.ui.components.AIRephrasePopup;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.ToastManager;
import com.connectai.ui.media.AttachmentPreviewDialog;
import com.connectai.ui.media.VoiceRecorderPanel;
import com.connectai.ui.poll.CreatePollDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ComposerPanel extends JPanel {
    private String conversationId;
    private Message replyToMessage;
    private Message editTargetMessage;

    private JTextArea inputArea;
    private JPanel previewHeaderPanel;
    private JPanel activeComposerCard;
    private Runnable onMessageSentCallback;
    private static final String PLACEHOLDER = "Type a message...";

    public ComposerPanel(Runnable onMessageSentCallback) {
        this.onMessageSentCallback = onMessageSentCallback;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 20, 16, 20));

        renderNormalComposer();
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setReplyToMessage(Message replyMsg) {
        this.replyToMessage = replyMsg;
        renderReplyOrEditPreview("Replying to: " + replyMsg.getContent());
    }

    public void setEditTargetMessage(Message editMsg) {
        this.editTargetMessage = editMsg;
        inputArea.setText(editMsg.getContent());
        inputArea.setForeground(ThemeColors.PRIMARY_TEXT);
        renderReplyOrEditPreview("Editing message: " + editMsg.getContent());
    }

    private void renderNormalComposer() {
        removeAll();

        activeComposerCard = new RoundedPanel(24, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
        activeComposerCard.setLayout(new BorderLayout(8, 0));
        activeComposerCard.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 8));

        // Reply / Edit preview header
        previewHeaderPanel = new JPanel(new BorderLayout());
        previewHeaderPanel.setOpaque(false);
        previewHeaderPanel.setVisible(false);

        inputArea = new JTextArea(1, 40);
        inputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputArea.setText(PLACEHOLDER);
        inputArea.setForeground(ThemeColors.MUTED_TEXT);
        inputArea.setCaretColor(ThemeColors.PRIMARY_ACCENT);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setOpaque(false);
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        inputArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (PLACEHOLDER.equals(inputArea.getText())) {
                    inputArea.setText("");
                    inputArea.setForeground(ThemeColors.PRIMARY_TEXT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputArea.getText().trim().isEmpty()) {
                    inputArea.setText(PLACEHOLDER);
                    inputArea.setForeground(ThemeColors.MUTED_TEXT);
                }
            }
        });

        inputArea.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        inputArea.append("\n");
                    } else {
                        e.consume();
                        sendCurrentText();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(inputArea);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(500, 36));

        // Left tool: 📎 Paperclip
        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftTools.setOpaque(false);

        IconButton attachBtn = new IconButton("📎", "Attach File");
        attachBtn.setIconColor(ThemeColors.SECONDARY_TEXT);
        attachBtn.addActionListener(e -> selectAndAttachFile());

        leftTools.add(attachBtn);

        IconButton pollBtn = new IconButton("📊", "Create Poll");
        pollBtn.setIconColor(ThemeColors.PRIMARY_ACCENT);
        pollBtn.addActionListener(e -> {
            if (conversationId == null) {
                ToastManager.showToast(this, "Open a conversation before creating a poll",
                        ToastManager.ToastType.WARNING);
                return;
            }
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            new CreatePollDialog(frame, conversationId, onMessageSentCallback).setVisible(true);
        });
        leftTools.add(pollBtn);

        // Right tools: ✨ AI Rephrase, 🎙 Microphone, ✈ Send Button
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightTools.setOpaque(false);

        IconButton aiBtn = new IconButton("✨", "AI Rephrase");
        aiBtn.setIconColor(ThemeColors.PRIMARY_ACCENT);
        aiBtn.addActionListener(e -> openAIRephrasePopup(aiBtn));

        IconButton micBtn = new IconButton("🎙", "Record Voice Note");
        micBtn.setIconColor(ThemeColors.SECONDARY_TEXT);
        micBtn.addActionListener(e -> startVoiceRecorder());

        rightTools.add(aiBtn);
        // Solid vibrant blue circular send button with white paper airplane
        JButton circularSendBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 4;
                g2.setColor(ThemeColors.PRIMARY_ACCENT);
                g2.fillOval(2, 2, size, size);

                // White paper plane / arrow
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                g2.drawString("✈", 10, 24);
                g2.dispose();
            }
        };
        circularSendBtn.setPreferredSize(new Dimension(38, 38));
        circularSendBtn.setFocusPainted(false);
        circularSendBtn.setBorderPainted(false);
        circularSendBtn.setContentAreaFilled(false);
        circularSendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        circularSendBtn.setToolTipText("Send Message");
        circularSendBtn.addActionListener(e -> sendCurrentText());

        rightTools.add(circularSendBtn);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(previewHeaderPanel, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);

        activeComposerCard.add(leftTools, BorderLayout.WEST);
        activeComposerCard.add(centerPanel, BorderLayout.CENTER);
        activeComposerCard.add(rightTools, BorderLayout.EAST);

        add(activeComposerCard, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void renderReplyOrEditPreview(String text) {
        previewHeaderPanel.removeAll();
        previewHeaderPanel.setVisible(true);

        JLabel label = new JLabel(text);
        label.setFont(ThemeFonts.CAPTION);
        label.setForeground(ThemeColors.PRIMARY_ACCENT);

        IconButton cancelBtn = new IconButton("✕", "Cancel");
        cancelBtn.setPreferredSize(new Dimension(20, 20));
        cancelBtn.addActionListener(e -> clearPreview());

        previewHeaderPanel.add(label, BorderLayout.CENTER);
        previewHeaderPanel.add(cancelBtn, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    private void clearPreview() {
        replyToMessage = null;
        editTargetMessage = null;
        previewHeaderPanel.setVisible(false);
        inputArea.setText(PLACEHOLDER);
        inputArea.setForeground(ThemeColors.MUTED_TEXT);
        revalidate();
        repaint();
    }

    private void sendCurrentText() {
        String content = inputArea.getText().trim();
        if (content.isEmpty() || PLACEHOLDER.equals(content) || conversationId == null) {
            return;
        }

        if (editTargetMessage != null) {
            ChatService.getInstance().editMessage(editTargetMessage.getId(), content).thenRun(() -> {
                SwingUtilities.invokeLater(() -> {
                    clearPreview();
                    if (onMessageSentCallback != null) onMessageSentCallback.run();
                });
            }).exceptionally(error -> {
                SwingUtilities.invokeLater(() -> ToastManager.showToast(this,
                        com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR));
                return null;
            });
            return;
        }

        String replyId = replyToMessage != null ? replyToMessage.getId() : null;
        ChatService.getInstance().sendTextMessage(conversationId, content, replyId).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                inputArea.setText("");
                clearPreview();
                inputArea.requestFocusInWindow();
                if (onMessageSentCallback != null) onMessageSentCallback.run();
            });
        }).exceptionally(error -> {
            SwingUtilities.invokeLater(() -> ToastManager.showToast(this,
                    com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR));
            return null;
        });
    }

    private void startVoiceRecorder() {
        removeAll();
        VoiceRecorderPanel recorder = new VoiceRecorderPanel(
                () -> renderNormalComposer(),
                (audioBytes, durationSeconds) -> {
                    if (audioBytes != null && audioBytes.length > 0 && conversationId != null) {
                        AudioService.getInstance().sendVoiceNote(conversationId, audioBytes, durationSeconds)
                                .whenComplete((message, error) -> SwingUtilities.invokeLater(() -> {
                                    renderNormalComposer();
                                    if (error != null) ToastManager.showToast(this,
                                            com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR);
                                    else if (onMessageSentCallback != null) onMessageSentCallback.run();
                                }));
                    } else {
                        renderNormalComposer();
                        ToastManager.showToast(this, "The recording is empty or no conversation is selected.",
                                ToastManager.ToastType.WARNING);
                    }
                }
        );
        add(recorder, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void selectAndAttachFile() {
        if (conversationId == null) return;
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            Frame top = (Frame) SwingUtilities.getWindowAncestor(this);
            new AttachmentPreviewDialog(top, selected, (f, caption) -> {
                ToastManager.showToast(this, "Uploading " + f.getName() + "…", ToastManager.ToastType.INFO);
                AttachmentService.getInstance().uploadAndSendFile(conversationId, f, caption, null, 0).thenRun(() -> {
                    SwingUtilities.invokeLater(() -> {
                        if (onMessageSentCallback != null) onMessageSentCallback.run();
                    });
                }).exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> ToastManager.showToast(this,
                            com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR));
                    return null;
                });
            }).setVisible(true);
        }
    }

    private void openAIRephrasePopup(IconButton anchor) {
        String text = inputArea.getText().trim();
        if (text.isEmpty() || PLACEHOLDER.equals(text)) {
            ToastManager.showToast(this, "Type a message first to rephrase it with AI", ToastManager.ToastType.WARNING);
            return;
        }

        AIRephrasePopup popup = new AIRephrasePopup(text, selectedRephrase -> {
            inputArea.setText(selectedRephrase);
            inputArea.setForeground(ThemeColors.PRIMARY_TEXT);
        });
        popup.show(anchor, 0, -260);
    }
}
