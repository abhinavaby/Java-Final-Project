package com.connectai.ui.media;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Attachment;
import com.connectai.service.AttachmentService;
import com.connectai.service.AudioService;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.util.HttpClientUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AudioPlayerComponent extends JPanel {
    private Attachment attachment;
    private IconButton playBtn;
    private boolean isPlaying = false;

    public AudioPlayerComponent(Attachment attachment) {
        this.attachment = attachment;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel card = new RoundedPanel(10, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        card.setPreferredSize(new Dimension(260, 48));

        playBtn = new IconButton("▶", "Play Audio");
        playBtn.addActionListener(e -> togglePlay());

        JLabel nameLabel = new JLabel(attachment.getOriginalFilename());
        nameLabel.setFont(ThemeFonts.BODY_MEDIUM);
        nameLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        String dur = attachment.getDurationSeconds() > 0
                ? String.format("%02d:%02d", attachment.getDurationSeconds() / 60, attachment.getDurationSeconds() % 60)
                : attachment.getFormattedSize();

        JLabel infoLabel = new JLabel(dur);
        infoLabel.setFont(ThemeFonts.CAPTION);
        infoLabel.setForeground(ThemeColors.MUTED_TEXT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(infoLabel);

        card.add(playBtn, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    private void togglePlay() {
        if (isPlaying) {
            AudioService.getInstance().stopPlayback();
            isPlaying = false;
            playBtn.setText("▶");
        } else {
            isPlaying = true;
            playBtn.setText("⏸");

            AttachmentService.getInstance().getSignedDownloadUrl(attachment.getStoragePath())
                    .thenCompose(url -> HttpClientUtil.getBytesAsync(url, null))
                    .thenAccept(response -> {
                        if (response.statusCode() < 400) {
                            byte[] bytes = response.body();
                            AudioService.getInstance().playAudio(bytes, () -> {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    isPlaying = false;
                                    playBtn.setText("▶");
                                });
                            });
                        } else {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                isPlaying = false;
                                playBtn.setText("▶");
                                com.connectai.ui.components.ToastManager.showToast(this,
                                        "Audio download failed (HTTP " + response.statusCode() + ").",
                                        com.connectai.ui.components.ToastManager.ToastType.ERROR);
                            });
                        }
                    }).exceptionally(ex -> {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            isPlaying = false;
                            playBtn.setText("▶");
                            com.connectai.ui.components.ToastManager.showToast(this,
                                    com.connectai.util.ErrorMessages.from(ex),
                                    com.connectai.ui.components.ToastManager.ToastType.ERROR);
                        });
                        return null;
                    });
        }
    }
}
