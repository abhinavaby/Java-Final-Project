package com.connectai.ui.group;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.service.ChatService;
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

public class JoinGroupDialog extends JDialog {
    private RoundedTextField codeField;
    private Runnable onSuccessCallback;

    public JoinGroupDialog(Frame owner, Runnable onSuccessCallback) {
        super(owner, "Join Group by Invite Code", true);
        this.onSuccessCallback = onSuccessCallback;
        setSize(new Dimension(420, 240));
        setLocationRelativeTo(owner);

        JPanel mainPanel = new RoundedPanel(0, ThemeColors.MAIN_BG);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("🔑 Join Group with Code");
        title.setFont(ThemeFonts.TITLE_MEDIUM);
        title.setForeground(ThemeColors.PRIMARY_TEXT);

        codeField = new RoundedTextField("Enter 8-character invite code");

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(codeField);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        PremiumButton cancelBtn = new PremiumButton("Cancel", ThemeColors.ELEVATED_SURFACE, ThemeColors.PRIMARY_TEXT);
        cancelBtn.addActionListener(e -> dispose());

        PremiumButton joinBtn = new PremiumButton("Join Group", ThemeColors.PRIMARY_ACCENT, ThemeColors.ACCENT_TEXT);
        joinBtn.addActionListener(e -> submit(joinBtn));

        footer.add(cancelBtn, BorderLayout.WEST);
        footer.add(joinBtn, BorderLayout.EAST);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void submit(PremiumButton btn) {
        String code = codeField.getText().trim();
        if (code.isBlank()) {
            ToastManager.showToast(this, "Please enter an invite code", ToastManager.ToastType.WARNING);
            return;
        }

        btn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ChatService.getInstance().joinGroupByCode(code).get();
                return null;
            }

            @Override
            protected void done() {
                btn.setEnabled(true);
                try {
                    get();
                    ToastManager.showToast(JoinGroupDialog.this, "Joined group successfully!",
                            ToastManager.ToastType.SUCCESS);
                    dispose();
                    if (onSuccessCallback != null)
                        onSuccessCallback.run();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    String message = com.connectai.util.ErrorMessages.from(cause);
                    ToastManager.showToast(JoinGroupDialog.this, "Failed to join group: " + message,
                            ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }
}
