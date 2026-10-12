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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class CreateGroupDialog extends JDialog {
    private RoundedTextField nameField;
    private RoundedTextField descField;
    private JCheckBox privateCheck;
    private Runnable onSuccessCallback;

    public CreateGroupDialog(Frame owner, Runnable onSuccessCallback) {
        super(owner, "Create New Group", true);
        this.onSuccessCallback = onSuccessCallback;
        setSize(new Dimension(460, 360));
        setLocationRelativeTo(owner);

        JPanel mainPanel = new RoundedPanel(0, ThemeColors.MAIN_BG);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("👥 Create New Group Chat");
        title.setFont(ThemeFonts.TITLE_MEDIUM);
        title.setForeground(ThemeColors.PRIMARY_TEXT);

        nameField = new RoundedTextField("Group Name");
        descField = new RoundedTextField("Description (optional)");

        privateCheck = new JCheckBox("Make group private (invite code required)", false);
        privateCheck.setFont(ThemeFonts.BODY_MEDIUM);
        privateCheck.setForeground(ThemeColors.PRIMARY_TEXT);
        privateCheck.setOpaque(false);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        center.add(nameField);
        center.add(javax.swing.Box.createVerticalStrut(12));
        center.add(descField);
        center.add(javax.swing.Box.createVerticalStrut(14));
        center.add(privateCheck);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        PremiumButton cancelBtn = new PremiumButton("Cancel", ThemeColors.ELEVATED_SURFACE, ThemeColors.PRIMARY_TEXT);
        cancelBtn.addActionListener(e -> dispose());

        PremiumButton createBtn = new PremiumButton("Create Group", ThemeColors.PRIMARY_ACCENT,
                ThemeColors.ACCENT_TEXT);
        createBtn.addActionListener(e -> submit(createBtn));

        footer.add(cancelBtn, BorderLayout.WEST);
        footer.add(createBtn, BorderLayout.EAST);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void submit(PremiumButton btn) {
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();
        boolean isPrivate = privateCheck.isSelected();

        if (name.isBlank()) {
            ToastManager.showToast(this, "Please enter a group name", ToastManager.ToastType.WARNING);
            return;
        }

        btn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ChatService.getInstance().createGroup(name, desc, isPrivate).get();
                return null;
            }

            @Override
            protected void done() {
                btn.setEnabled(true);
                try {
                    get();
                    ToastManager.showToast(CreateGroupDialog.this, "Group created successfully!",
                            ToastManager.ToastType.SUCCESS);
                    dispose();
                    if (onSuccessCallback != null)
                        onSuccessCallback.run();
                } catch (Exception ex) {
                    ToastManager.showToast(CreateGroupDialog.this, com.connectai.util.ErrorMessages.from(ex),
                            ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }
}
