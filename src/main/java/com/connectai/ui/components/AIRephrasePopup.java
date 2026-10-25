package com.connectai.ui.components;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Tone;
import com.connectai.service.AIService;

import java.awt.BorderLayout;
import java.awt.Component;

import java.util.function.Consumer;

import javax.swing.BorderFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class AIRephrasePopup extends JPopupMenu {
    private String currentDraft;
    private Consumer<String> onApplyCallback;

    public AIRephrasePopup(String draftText, Consumer<String> onApplyCallback) {
        this.currentDraft = draftText;
        this.onApplyCallback = onApplyCallback;

        setBackground(ThemeColors.ELEVATED_SURFACE);
        setBorder(BorderFactory.createLineBorder(ThemeColors.PRIMARY_ACCENT, 1));

        JPanel panel = new RoundedPanel(12, ThemeColors.ELEVATED_SURFACE);
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("✨ Rephrase with AI Tone");
        title.setFont(ThemeFonts.BODY_BOLD);
        title.setForeground(ThemeColors.PRIMARY_ACCENT);
        panel.add(title, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new BorderLayout(0, 6));
        optionsPanel.setOpaque(false);
        JPanel chipsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 6));
        chipsPanel.setOpaque(false);

        for (Tone tone : Tone.values()) {
            PremiumButton btn = new PremiumButton(tone.getDisplayName(), ThemeColors.MAIN_BG, ThemeColors.PRIMARY_TEXT);
            btn.setFont(ThemeFonts.BODY_SMALL);
            btn.addActionListener(e -> rephrase(tone, panel));
            chipsPanel.add(btn);
        }

        optionsPanel.add(chipsPanel, BorderLayout.CENTER);
        if (!com.connectai.config.AppConfig.isMockMode()) {
            JLabel privacyNotice = new JLabel("Your draft is sent to the configured AI service through Supabase.");
            privacyNotice.setFont(ThemeFonts.CAPTION);
            privacyNotice.setForeground(ThemeColors.MUTED_TEXT);
            optionsPanel.add(privacyNotice, BorderLayout.SOUTH);
        }
        panel.add(optionsPanel, BorderLayout.CENTER);
        add(panel);
    }

    private void rephrase(Tone tone, JPanel parentPanel) {
        JLabel loading = new JLabel("Rephrasing draft...");
        loading.setFont(ThemeFonts.BODY_SMALL);
        loading.setForeground(ThemeColors.SECONDARY_ACCENT);

        parentPanel.add(loading, BorderLayout.SOUTH);
        parentPanel.revalidate();
        parentPanel.repaint();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return AIService.getInstance().rephraseMessage(currentDraft, tone).get().getRephrasedText();
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (onApplyCallback != null && result != null) {
                        onApplyCallback.accept(result);
                    }
                } catch (Exception ex) {
                    ToastManager.showToast(parentPanel, "Failed to rephrase draft", ToastManager.ToastType.ERROR);
                } finally {
                    setVisible(false);
                }
            }
        }.execute();
    }
}
