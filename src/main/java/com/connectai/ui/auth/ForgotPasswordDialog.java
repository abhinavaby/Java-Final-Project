package com.connectai.ui.auth;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.repository.AuthRepository;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.RoundedTextField;
import com.connectai.ui.components.ToastManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class ForgotPasswordDialog extends JDialog {
    private RoundedTextField emailField;

    public ForgotPasswordDialog(Frame owner) {
        super(owner, "Reset Password", true);
        setSize(new Dimension(420, 260));
        setLocationRelativeTo(owner);

        JPanel mainPanel = new RoundedPanel(0, ThemeColors.MAIN_BG);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("🔑 Reset Your Password");
        title.setFont(ThemeFonts.TITLE_MEDIUM);
        title.setForeground(ThemeColors.PRIMARY_TEXT);

        JLabel info = new JLabel("<html>Enter your account email. We’ll send a password reset link.</html>");
        info.setFont(ThemeFonts.BODY_MEDIUM);
        info.setForeground(ThemeColors.SECONDARY_TEXT);

        emailField = new RoundedTextField("Enter your email");

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(info);
        center.add(javax.swing.Box.createVerticalStrut(16));
        center.add(emailField);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);

        PremiumButton cancelBtn = new PremiumButton("Cancel", ThemeColors.ELEVATED_SURFACE, ThemeColors.PRIMARY_TEXT);
        cancelBtn.addActionListener(e -> dispose());

        PremiumButton submitBtn = new PremiumButton("Send Reset Link", ThemeColors.PRIMARY_ACCENT,
                ThemeColors.ACCENT_TEXT);
        submitBtn.addActionListener(e -> sendReset(submitBtn));

        footer.add(cancelBtn, BorderLayout.WEST);
        footer.add(submitBtn, BorderLayout.EAST);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void sendReset(PremiumButton btn) {
        String email = emailField.getText().trim();
        if (email.isBlank()) {
            ToastManager.showToast(this, "Please enter your email", ToastManager.ToastType.WARNING);
            return;
        }

        btn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return new AuthRepository().sendPasswordReset(email).get();
            }

            @Override
            protected void done() {
                btn.setEnabled(true);
                try {
                    if (get()) {
                        ToastManager.showToast(getOwner(), "If this email has an account, a reset link will arrive shortly.",
                                ToastManager.ToastType.SUCCESS);
                        dispose();
                    } else {
                        ToastManager.showToast(ForgotPasswordDialog.this, "Failed to send reset link",
                                ToastManager.ToastType.ERROR);
                    }
                } catch (Exception ex) {
                    ToastManager.showToast(ForgotPasswordDialog.this, com.connectai.util.ErrorMessages.from(ex),
                            ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }
}
