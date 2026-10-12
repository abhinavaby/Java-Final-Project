package com.connectai.ui.components;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ToastManager {
    public enum ToastType {
        SUCCESS(ThemeColors.SUCCESS_ONLINE, "✓"),
        ERROR(ThemeColors.ERROR, "✕"),
        WARNING(ThemeColors.WARNING, "⚠"),
        INFO(ThemeColors.SECONDARY_ACCENT, "ℹ");

        final Color color;
        final String icon;

        ToastType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    public static void showToast(Component parent, String message, ToastType type) {
        if (parent == null) return;

        javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(parent);
        if (root == null) return;
        javax.swing.JLayeredPane layer = root.getLayeredPane();

        RoundedPanel toastPanel = new RoundedPanel(12, ThemeColors.ELEVATED_SURFACE, type.color);
        toastPanel.setLayout(new BorderLayout(10, 0));
        toastPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel iconLabel = new JLabel(type.icon);
        iconLabel.setFont(ThemeFonts.TITLE_MEDIUM);
        iconLabel.setForeground(type.color);

        JLabel msgLabel = new JLabel("<html>" + escapeHtml(message) + "</html>");
        msgLabel.setFont(ThemeFonts.BODY_MEDIUM);
        msgLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        toastPanel.add(iconLabel, BorderLayout.WEST);
        toastPanel.add(msgLabel, BorderLayout.CENTER);

        int width = Math.max(200, Math.min(460, layer.getWidth() - 32));
        msgLabel.setSize(width - 72, Short.MAX_VALUE);
        int height = Math.max(48, msgLabel.getPreferredSize().height + 24);
        toastPanel.setSize(new Dimension(width, height));
        int x = Math.max(8, layer.getWidth() - width - 16);
        int y = 40;
        toastPanel.setLocation(x, y);

        layer.add(toastPanel, javax.swing.JLayeredPane.POPUP_LAYER);
        layer.repaint();

        new javax.swing.Timer(type == ToastType.ERROR ? 10000 : 5000, e -> {
            layer.remove(toastPanel);
            layer.repaint();
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
