package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Message;
import com.connectai.model.MessageReaction;
import com.connectai.model.MessageType;
import com.connectai.ui.components.AvatarComponent;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.ToastManager;
import com.connectai.ui.media.AudioPlayerComponent;
import com.connectai.ui.poll.PollCardComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Clean, modern, uncluttered message bubble component.
 */
public class MessageBubble extends JPanel {
    private Message message;
    private boolean isOutgoing;
    private Consumer<Message> onReplyCallback;
    private Consumer<Message> onEditCallback;
    private Consumer<Message> onDeleteCallback;
    private Consumer<String> onReactionCallback;

    public MessageBubble(Message message, boolean isOutgoing, Consumer<Message> onReplyCallback,
                         Consumer<Message> onEditCallback, Consumer<Message> onDeleteCallback) {
        this(message, isOutgoing, onReplyCallback, onEditCallback, onDeleteCallback, null);
    }

    public MessageBubble(Message message, boolean isOutgoing, Consumer<Message> onReplyCallback,
                         Consumer<Message> onEditCallback, Consumer<Message> onDeleteCallback,
                         Consumer<String> onReactionCallback) {
        this.message = message;
        this.isOutgoing = isOutgoing;
        this.onReplyCallback = onReplyCallback;
        this.onEditCallback = onEditCallback;
        this.onDeleteCallback = onDeleteCallback;
        this.onReactionCallback = onReactionCallback;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 18, 4, 18));

        String currentName = "You";
        com.connectai.model.Profile p = com.connectai.service.AuthService.getInstance().getCurrentProfile();
        if (p != null && p.getDisplayName() != null && !p.getDisplayName().isBlank()) {
            currentName = p.getDisplayName();
        } else if (com.connectai.service.AuthService.getInstance().getCurrentUser() != null
                && com.connectai.service.AuthService.getInstance().getCurrentUser().getEmail() != null) {
            String prefix = com.connectai.service.AuthService.getInstance().getCurrentUser().getEmail().split("@")[0];
            currentName = Character.toUpperCase(prefix.charAt(0)) + (prefix.length() > 1 ? prefix.substring(1) : "");
        }

        String senderName = message.getSenderProfile() != null
                ? message.getSenderProfile().getEffectiveName()
                : (isOutgoing ? currentName : "Member");

        String timeStr = formatTime(message.getCreatedAt());

        if (isOutgoing) {
            renderOutgoing(senderName, timeStr);
        } else {
            renderIncoming(senderName, timeStr);
        }
    }

    private void renderIncoming(String senderName, String timeStr) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(false);

        // 1. Avatar (Top-aligned)
        JPanel avatarBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avatarBox.setOpaque(false);
        AvatarComponent avatar = new AvatarComponent(34, senderName, false);
        avatarBox.add(avatar);
        wrapper.add(avatarBox, BorderLayout.WEST);

        // 2. Bubble Body & Reactions Column
        JPanel bubbleCol = new JPanel();
        bubbleCol.setLayout(new BoxLayout(bubbleCol, BoxLayout.Y_AXIS));
        bubbleCol.setOpaque(false);
        bubbleCol.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Bubble card
        JPanel card = new RoundedPanel(14, ThemeColors.INCOMING_BUBBLE, ThemeColors.CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header: Sender Name + Timestamp
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(senderName);
        nameLabel.setFont(ThemeFonts.BODY_BOLD);
        nameLabel.setForeground(ThemeColors.PRIMARY_ACCENT);

        JLabel timeLabel = new JLabel("  " + timeStr);
        timeLabel.setFont(ThemeFonts.CAPTION);
        timeLabel.setForeground(ThemeColors.MUTED_TEXT);

        header.add(nameLabel);
        header.add(timeLabel);
        header.add(Box.createHorizontalGlue());

        card.add(header);
        card.add(Box.createVerticalStrut(4));

        // Content
        addContent(card, false);

        bubbleCol.add(card);

        // Only show reaction bar if message actually has reactions (avoids visual clutter!)
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            bubbleCol.add(Box.createVerticalStrut(4));
            bubbleCol.add(createExistingReactionsBar());
        }

        wrapper.add(bubbleCol, BorderLayout.CENTER);
        add(wrapper, BorderLayout.WEST);

        setupContextMenu(card);
    }

    private void renderOutgoing(String senderName, String timeStr) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrapper.setOpaque(false);

        JPanel card = new RoundedPanel(14, ThemeColors.OUTGOING_BUBBLE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        card.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Content
        addContent(card, true);

        // Timestamp & checkmarks
        JLabel footerLabel = new JLabel(timeStr + " ✓✓");
        footerLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        footerLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        footerLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        card.add(Box.createVerticalStrut(4));
        card.add(footerLabel);

        wrapper.add(card);
        add(wrapper, BorderLayout.EAST);

        setupContextMenu(card);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private void addContent(JPanel container, boolean isOutgoingMsg) {
        if (message.getAttachment() != null && message.getMessageType() != MessageType.AUDIO && message.getMessageType() != MessageType.VOICE) {
            com.connectai.model.Attachment attachment = message.getAttachment();
            javax.swing.JButton open = new javax.swing.JButton(attachment.getOriginalFilename());
            open.setFont(ThemeFonts.BODY_MEDIUM);
            open.setAlignmentX(Component.LEFT_ALIGNMENT);
            open.setToolTipText("Open attachment");
            open.addActionListener(event -> com.connectai.service.AttachmentService.getInstance()
                    .getSignedDownloadUrl(attachment.getStoragePath()).thenAccept(url -> {
                        try {
                            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                        } catch (Exception error) {
                            javax.swing.SwingUtilities.invokeLater(() -> ToastManager.showToast(this,
                                    "Could not open attachment: " + error.getMessage(), ToastManager.ToastType.ERROR));
                        }
                    }).exceptionally(error -> {
                        javax.swing.SwingUtilities.invokeLater(() -> ToastManager.showToast(this,
                                com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR));
                        return null;
                    }));
            if (attachment.getMimeType() != null && attachment.getMimeType().startsWith("image/")) {
                JLabel preview = new JLabel("Loading photo…");
                preview.setAlignmentX(Component.LEFT_ALIGNMENT);
                preview.setForeground(ThemeColors.SECONDARY_TEXT);
                container.add(preview);
                com.connectai.service.AttachmentService.getInstance().getSignedDownloadUrl(attachment.getStoragePath())
                        .thenApplyAsync(url -> {
                            try {
                                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(java.net.URI.create(url).toURL());
                                if (img == null) throw new IllegalStateException("Unsupported image format");
                                double scale = Math.min(1.0, Math.min(240.0 / img.getWidth(), 180.0 / img.getHeight()));
                                return new javax.swing.ImageIcon(img.getScaledInstance((int)(img.getWidth()*scale),
                                        (int)(img.getHeight()*scale), java.awt.Image.SCALE_SMOOTH));
                            } catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
                        }).whenComplete((icon, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
                            preview.setText(error == null ? "" : "Open file to view photo");
                            if (icon != null) preview.setIcon(icon);
                            revalidate();
                            repaint();
                        }));
            }
            container.add(open);
        }
        if (message.getMessageType() == MessageType.POLL && message.getPoll() != null) {
            JPanel p = new PollCardComponent(message.getPoll());
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(p);
        } else if ((message.getMessageType() == MessageType.AUDIO || message.getMessageType() == MessageType.VOICE)
                && message.getAttachment() != null) {
            JPanel a = new AudioPlayerComponent(message.getAttachment());
            a.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(a);
        } else {
            String raw = message.getContent() != null ? message.getContent() : "";
            raw = raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            String formatted = raw.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                    .replaceAll("\\n", "<br/>");

            Color text = isOutgoing ? ThemeColors.OUTGOING_TEXT : ThemeColors.INCOMING_TEXT;
            String textColor = String.format("#%02X%02X%02X", text.getRed(), text.getGreen(), text.getBlue());
            int width = Math.min(280, Math.max(24, getFontMetrics(ThemeFonts.BODY_MEDIUM).stringWidth(raw) + 4));
            JLabel label = new JLabel("<html><body style='width: " + width + "px; color: " + textColor + "; font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; font-size: 13px; line-height: 1.4;'>" + formatted + "</body></html>");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(label);
        }
    }

    private JPanel createExistingReactionsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (MessageReaction rx : message.getReactions()) {
            JPanel pill = new RoundedPanel(12, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
            pill.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 2));
            JLabel el = new JLabel(rx.getEmoji());
            el.setFont(ThemeFonts.CAPTION);
            el.setForeground(ThemeColors.SECONDARY_TEXT);
            pill.add(el);
            bar.add(pill);
        }
        return bar;
    }

    private void setupContextMenu(JPanel target) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem copy = new JMenuItem("📋 Copy Text");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(message.getContent()), null);
            ToastManager.showToast(this, "Copied to clipboard", ToastManager.ToastType.SUCCESS);
        });

        JMenuItem reply = new JMenuItem("↩ Reply");
        reply.addActionListener(e -> {
            if (onReplyCallback != null) onReplyCallback.accept(message);
        });

        JMenuItem reactHeart = new JMenuItem("❤️ React Heart");
        reactHeart.addActionListener(e -> react("❤️"));

        JMenuItem reactThumbs = new JMenuItem("👍 React Thumbs Up");
        reactThumbs.addActionListener(e -> react("👍"));

        menu.add(copy);
        menu.add(reply);
        menu.addSeparator();
        menu.add(reactHeart);
        menu.add(reactThumbs);

        if (isOutgoing) {
            JMenuItem edit = new JMenuItem("✏ Edit");
            edit.addActionListener(e -> {
                if (onEditCallback != null) onEditCallback.accept(message);
            });
            JMenuItem delete = new JMenuItem("🗑 Delete");
            delete.addActionListener(e -> {
                if (onDeleteCallback != null) onDeleteCallback.accept(message);
            });
            menu.addSeparator();
            menu.add(edit);
            menu.add(delete);
        }

        target.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(target, e.getX(), e.getY());
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(target, e.getX(), e.getY());
            }
        });
    }

    private void react(String emoji) {
        if (onReactionCallback != null) onReactionCallback.accept(emoji);
    }

    private String formatTime(String timeIso) {
        if (timeIso == null || timeIso.isBlank()) return "";
        try {
            java.time.Instant instant = java.time.Instant.parse(timeIso);
            return java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                    .withZone(java.time.ZoneId.systemDefault()).format(instant);
        } catch (java.time.format.DateTimeParseException ignored) {
            try {
                return java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                        .format(java.time.OffsetDateTime.parse(timeIso).atZoneSameInstant(java.time.ZoneId.systemDefault()));
            } catch (java.time.format.DateTimeParseException ignoredAgain) { return ""; }
        }
    }
}
