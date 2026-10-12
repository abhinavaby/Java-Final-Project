package com.connectai.ui.components;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeDimensions;
import com.connectai.config.ThemeFonts;
import com.connectai.config.ThemeSpacing;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class PasswordFieldWithToggle extends JPanel implements FocusListener {
    private JPasswordField passwordField;
    private IconButton toggleButton;
    private boolean showPassword = false;
    private boolean isFocused = false;
    private String placeholder;

    public PasswordFieldWithToggle(String placeholder) {
        this.placeholder = placeholder;
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(340, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        passwordField = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocused && placeholder != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ThemeColors.MUTED_TEXT);
                    g2.setFont(getFont());
                    int textY = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
                    g2.drawString(placeholder, getInsets().left, textY);
                    g2.dispose();
                }
            }
        };

        passwordField.setFont(ThemeFonts.BODY_MEDIUM);
        passwordField.setForeground(ThemeColors.PRIMARY_TEXT);
        passwordField.setCaretColor(ThemeColors.PRIMARY_ACCENT);
        passwordField.setBackground(ThemeColors.ELEVATED_SURFACE);
        passwordField.setOpaque(false);
        passwordField.setBorder(BorderFactory.createEmptyBorder(ThemeSpacing.S, ThemeSpacing.M, ThemeSpacing.S, ThemeSpacing.M));
        passwordField.addFocusListener(this);

        toggleButton = new IconButton("Show", "Toggle password visibility");
        toggleButton.addActionListener(e -> {
            showPassword = !showPassword;
            if (showPassword) {
                passwordField.setEchoChar((char) 0);
                toggleButton.setText("Hide");
            } else {
                passwordField.setEchoChar('•');
                toggleButton.setText("Show");
            }
        });

        add(passwordField, BorderLayout.CENTER);
        add(toggleButton, BorderLayout.EAST);
    }

    public String getPasswordString() {
        return new String(passwordField.getPassword());
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    @Override
    public void focusGained(FocusEvent e) {
        isFocused = true;
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        isFocused = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(ThemeColors.ELEVATED_SURFACE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ThemeDimensions.CORNER_RADIUS, ThemeDimensions.CORNER_RADIUS);

        g2.setColor(isFocused ? ThemeColors.PRIMARY_ACCENT : ThemeColors.CARD_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ThemeDimensions.CORNER_RADIUS, ThemeDimensions.CORNER_RADIUS);
        g2.dispose();

        super.paintComponent(g);
    }
}
