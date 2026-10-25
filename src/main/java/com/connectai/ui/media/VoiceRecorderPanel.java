package com.connectai.ui.media;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.service.AudioService;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedPanel;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class VoiceRecorderPanel extends JPanel {
    private JLabel durationLabel;
    private Timer timer;
    private Runnable onCancelCallback;
    private java.util.function.BiConsumer<byte[], Integer> onSendCallback;
    private byte[] recordedWavBytes;
    private int durationSeconds;

    public VoiceRecorderPanel(Runnable onCancelCallback, java.util.function.BiConsumer<byte[], Integer> onSendCallback) {
        this.onCancelCallback = onCancelCallback;
        this.onSendCallback = onSendCallback;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel panel = new RoundedPanel(12, ThemeColors.ELEVATED_SURFACE, ThemeColors.ERROR);
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel micIcon = new JLabel("🎙 Recording...");
        micIcon.setFont(ThemeFonts.BODY_BOLD);
        micIcon.setForeground(ThemeColors.ERROR);

        durationLabel = new JLabel("00:00");
        durationLabel.setFont(ThemeFonts.BODY_BOLD);
        durationLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        IconButton cancelBtn = new IconButton("✕", "Cancel recording");
        cancelBtn.addActionListener(e -> cancel());

        PremiumButton stopSendBtn = new PremiumButton("Send Voice Note", ThemeColors.PRIMARY_ACCENT, ThemeColors.ACCENT_TEXT);
        stopSendBtn.setFont(ThemeFonts.BODY_SMALL);
        stopSendBtn.addActionListener(e -> stopAndSend());

        actions.add(cancelBtn);
        actions.add(stopSendBtn);

        panel.add(micIcon, BorderLayout.WEST);
        panel.add(durationLabel, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.EAST);

        add(panel, BorderLayout.CENTER);

        startTimer();
    }

    private void startTimer() {
        try {
            AudioService.getInstance().startVoiceRecording();
            timer = new Timer(1000, e -> {
                long sec = AudioService.getInstance().getRecordedDurationSeconds();
                durationSeconds = (int) sec;
                long min = sec / 60;
                long s = sec % 60;
                durationLabel.setText(String.format("%02d:%02d", min, s));
            });
            timer.start();
        } catch (Exception ex) {
            com.connectai.ui.components.ToastManager.showToast(this,
                    "Could not start recording: " + com.connectai.util.ErrorMessages.from(ex),
                    com.connectai.ui.components.ToastManager.ToastType.ERROR);
            cancel();
        }
    }

    private void stopAndSend() {
        if (timer != null) timer.stop();
        recordedWavBytes = AudioService.getInstance().stopVoiceRecording();
        durationSeconds = (int) AudioService.getInstance().getRecordedDurationSeconds();
        if (onSendCallback != null) {
            onSendCallback.accept(recordedWavBytes, durationSeconds);
        }
    }

    private void cancel() {
        if (timer != null) timer.stop();
        AudioService.getInstance().stopVoiceRecording();
        if (onCancelCallback != null) onCancelCallback.run();
    }
}
