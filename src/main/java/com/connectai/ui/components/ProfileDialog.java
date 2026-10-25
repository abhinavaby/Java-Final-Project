package com.connectai.ui.components;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Profile;
import com.connectai.service.AuthService;
import com.connectai.service.ChatService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Lets the signed-in user edit their display name, status and picture. */
public final class ProfileDialog extends JDialog {
    private final JTextField nameField = new JTextField(28);
    private final JTextArea aboutField = new JTextArea(4, 28);
    private final AvatarComponent avatar = new AvatarComponent(88, "You", false);
    private final JLabel pictureStatus = new JLabel(" ");
    private final Runnable onSaved;
    private File selectedPicture;

    public ProfileDialog(Frame owner, Runnable onSaved) {
        super(owner, "Edit profile", true);
        this.onSaved = onSaved;
        setSize(new Dimension(440, 430));
        setLocationRelativeTo(owner);
        Profile profile = AuthService.getInstance().getCurrentProfile();
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(ThemeColors.MAIN_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Your profile");
        title.setFont(ThemeFonts.TITLE_MEDIUM);
        title.setForeground(ThemeColors.PRIMARY_TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JPanel photo = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        photo.setOpaque(false);
        if (profile != null) {
            avatar.setDisplayName(profile.getEffectiveName());
            avatar.loadAvatar(profile.getAvatarPath());
        }
        PremiumButton choose = new PremiumButton("Choose picture", ThemeColors.ELEVATED_SURFACE, ThemeColors.PRIMARY_TEXT);
        choose.addActionListener(e -> choosePicture());
        JPanel photoActions = new JPanel();
        photoActions.setOpaque(false);
        photoActions.setLayout(new javax.swing.BoxLayout(photoActions, javax.swing.BoxLayout.Y_AXIS));
        photoActions.add(choose);
        photoActions.add(pictureStatus);
        photo.add(avatar);
        photo.add(photoActions);

        JLabel nameLabel = fieldLabel("Display name");
        nameField.setText(profile == null ? "" : profile.getEffectiveName());
        JLabel aboutLabel = fieldLabel("About");
        aboutField.setLineWrap(true);
        aboutField.setWrapStyleWord(true);
        aboutField.setText(profile == null || profile.getAbout() == null ? "" : profile.getAbout());
        aboutField.setBackground(ThemeColors.ELEVATED_SURFACE);
        aboutField.setForeground(ThemeColors.PRIMARY_TEXT);

        form.add(title);
        form.add(javax.swing.Box.createVerticalStrut(18));
        form.add(photo);
        form.add(javax.swing.Box.createVerticalStrut(16));
        form.add(nameLabel);
        form.add(javax.swing.Box.createVerticalStrut(5));
        form.add(nameField);
        form.add(javax.swing.Box.createVerticalStrut(14));
        form.add(aboutLabel);
        form.add(javax.swing.Box.createVerticalStrut(5));
        form.add(new JScrollPane(aboutField));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        PremiumButton cancel = new PremiumButton("Cancel", ThemeColors.ELEVATED_SURFACE, ThemeColors.PRIMARY_TEXT);
        cancel.addActionListener(e -> dispose());
        PremiumButton save = new PremiumButton("Save profile", ThemeColors.PRIMARY_ACCENT, ThemeColors.ACCENT_TEXT);
        save.addActionListener(e -> saveProfile(save));
        footer.add(cancel);
        footer.add(save);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeFonts.BODY_BOLD);
        label.setForeground(ThemeColors.PRIMARY_TEXT);
        return label;
    }

    private void choosePicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a profile picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (PNG, JPG, WEBP)", "png", "jpg", "jpeg", "webp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        selectedPicture = chooser.getSelectedFile();
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(selectedPicture);
            if (image == null) throw new IllegalArgumentException("That file is not a supported image.");
            avatar.setImage(image);
            pictureStatus.setText(selectedPicture.getName());
        } catch (Exception ex) {
            selectedPicture = null;
            ToastManager.showToast(this, "Choose a valid PNG, JPG, or WEBP image.", ToastManager.ToastType.WARNING);
        }
    }

    private void saveProfile(PremiumButton save) {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            ToastManager.showToast(this, "Enter a display name.", ToastManager.ToastType.WARNING);
            return;
        }
        if (name.length() > 100 || aboutField.getText().length() > 500) {
            ToastManager.showToast(this, "Display name is limited to 100 characters and About to 500.", ToastManager.ToastType.WARNING);
            return;
        }
        save.setEnabled(false);
        java.util.concurrent.CompletableFuture<String> upload = selectedPicture == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null)
                : ChatService.getInstance().uploadProfilePicture(selectedPicture);
        upload.thenCompose(path -> ChatService.getInstance().updateCurrentProfile(name, aboutField.getText(), path))
                .whenComplete((profile, error) -> SwingUtilities.invokeLater(() -> {
                    save.setEnabled(true);
                    if (error != null) {
                        ToastManager.showToast(this, com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR);
                        return;
                    }
                    ToastManager.showToast(this, "Profile updated", ToastManager.ToastType.SUCCESS);
                    if (onSaved != null) onSaved.run();
                    dispose();
                }));
    }
}
