package com.connectai.ui.auth;

import com.connectai.config.AppConfig;
import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeDimensions;
import com.connectai.config.ThemeFonts;
import com.connectai.service.AuthService;
import com.connectai.ui.chat.MainChatFrame;
import com.connectai.ui.components.RoundedPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class SplashScreen extends JFrame {
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public SplashScreen() {
        setUndecorated(true);
        setSize(new Dimension(500, 320));
        setLocationRelativeTo(null);

        JPanel mainPanel = new RoundedPanel(16, ThemeColors.MAIN_BG, ThemeColors.PRIMARY_ACCENT);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new javax.swing.BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("" + AppConfig.getAppName(), SwingConstants.CENTER);
        logoLabel.setFont(ThemeFonts.TITLE_LARGE.deriveFont(32.0f));
        logoLabel.setForeground(ThemeColors.PRIMARY_ACCENT);
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel taglineLabel = new JLabel("Your team, in one place.", SwingConstants.CENTER);
        taglineLabel.setFont(ThemeFonts.BODY_MEDIUM);
        taglineLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        taglineLabel.setAlignmentX(CENTER_ALIGNMENT);

        statusLabel = new JLabel("Initializing application...", SwingConstants.CENTER);
        statusLabel.setFont(ThemeFonts.BODY_SMALL);
        statusLabel.setForeground(ThemeColors.MUTED_TEXT);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setForeground(ThemeColors.PRIMARY_ACCENT);
        progressBar.setBackground(ThemeColors.ELEVATED_SURFACE);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(400, 6));

        centerPanel.add(javax.swing.Box.createVerticalGlue());
        centerPanel.add(logoLabel);
        centerPanel.add(javax.swing.Box.createVerticalStrut(8));
        centerPanel.add(taglineLabel);
        centerPanel.add(javax.swing.Box.createVerticalStrut(32));
        centerPanel.add(statusLabel);
        centerPanel.add(javax.swing.Box.createVerticalStrut(12));
        centerPanel.add(progressBar);
        centerPanel.add(javax.swing.Box.createVerticalGlue());

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    public void startInitialization() {
        setVisible(true);

        new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Loading configuration...");
                Thread.sleep(300);

                publish("Restoring session...");
                try {
                    AuthService.getInstance().restoreSession().get();
                    publish("Session restored! Connecting...");
                    Thread.sleep(200);
                    return true;
                } catch (Exception e) {
                    publish("Please log in...");
                    Thread.sleep(200);
                    return false;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    boolean sessionValid = get();
                    dispose();
                    if (sessionValid) {
                        SwingUtilities.invokeLater(() -> new MainChatFrame().setVisible(true));
                    } else {
                        SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
                    }
                } catch (Exception e) {
                    dispose();
                    SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
                }
            }
        }.execute();
    }
}
