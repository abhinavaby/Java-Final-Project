package com.connectai.ui.auth;

import com.connectai.config.AppConfig;
import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.service.AuthService;
import com.connectai.ui.chat.MainChatFrame;
import com.connectai.ui.components.PasswordFieldWithToggle;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.RoundedTextField;
import com.connectai.ui.components.ToastManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Clean, organized, and user-friendly authentication screen.
 */
public class AuthFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardsPanel;

    // Login components
    private RoundedTextField loginEmailField;
    private PasswordFieldWithToggle loginPasswordField;
    private PremiumButton loginButton;

    // Register components
    private RoundedTextField regNameField;
    private RoundedTextField regUserField;
    private RoundedTextField regEmailField;
    private PasswordFieldWithToggle regPasswordField;
    private PasswordFieldWithToggle regConfirmPasswordField;
    private PremiumButton registerButton;

    public AuthFrame() {
        setTitle(AppConfig.getAppName() + " — Authentication");
        try (var iconStream = getClass().getResourceAsStream("/icons/connectai.png")) {
            if (iconStream != null) setIconImage(javax.imageio.ImageIO.read(iconStream));
        } catch (Exception ignored) {}
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(960, 720));
        setMinimumSize(new Dimension(900, 680));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeColors.MAIN_BG);

        // Left Branding & Highlights Panel (Clean white, 380px)
        mainPanel.add(createBrandingPanel(), BorderLayout.WEST);

        // Right Cards Panel (Centered Login / Register Cards)
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        cardsPanel.add(createLoginCard(), "LOGIN");
        cardsPanel.add(createRegisterCard(), "REGISTER");

        mainPanel.add(cardsPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);
        loginPasswordField.getPasswordField().addActionListener(e -> performLogin());
        regConfirmPasswordField.getPasswordField().addActionListener(e -> performRegistration());
    }

    private JPanel createBrandingPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeColors.ELEVATED_SURFACE);
        panel.setPreferredSize(new Dimension(380, 640));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeColors.DIVIDER),
                BorderFactory.createEmptyBorder(44, 40, 40, 40)
        ));

        // 1. Logo Header
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel("C");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 26));
        logoIcon.setForeground(ThemeColors.PRIMARY_ACCENT);

        JLabel logoText = new JLabel("ConnectAI");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 26));
        logoText.setForeground(ThemeColors.PRIMARY_TEXT);

        logoRow.add(logoIcon);
        logoRow.add(logoText);

        JLabel tagline = new JLabel("Your team, in one place");
        tagline.setFont(ThemeFonts.BODY_MEDIUM);
        tagline.setForeground(ThemeColors.MUTED_TEXT);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));

        panel.add(logoRow);
        panel.add(tagline);
        panel.add(Box.createVerticalStrut(36));

        JLabel headline = new JLabel("<html>Good conversations.<br>Less noise.</html>");
        headline.setFont(ThemeFonts.TITLE_LARGE.deriveFont(30f));
        headline.setForeground(ThemeColors.PRIMARY_TEXT);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(headline);
        panel.add(Box.createVerticalStrut(20));
        panel.add(createFeatureItem("", "A place for your team", "Keep conversations, files and decisions together."));
        panel.add(Box.createVerticalStrut(20));
        panel.add(createFeatureItem("", "A little help when you need it", "Catch up on a conversation or find the right words with AI."));

        panel.add(Box.createVerticalGlue());

        // 3. Supabase Cloud Status Row & Config Button
        JPanel badge = new RoundedPanel(12, ThemeColors.PRIMARY_ACCENT_LIGHT, Color.decode("#BFDBFE"));
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        badge.setMaximumSize(new Dimension(300, 32));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel badgeText = new JLabel(AppConfig.isSupabaseConfigured() ? "Connection settings" : "Set up connection");
        badgeText.setFont(ThemeFonts.CAPTION);
        badgeText.setForeground(ThemeColors.PRIMARY_ACCENT);
        badge.add(badgeText);

        badge.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showSupabaseConfigDialog();
            }
        });

        panel.add(badge);

        return panel;
    }

    private JPanel createFeatureItem(String icon, String title, String desc) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setOpaque(false);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(320, 52));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeFonts.BODY_BOLD);
        titleLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        JLabel descLabel = new JLabel("<html><body style='width: 230px; color: #64748B; font-size: 11px;'>" + desc + "</body></html>");

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(descLabel);

        item.add(iconLabel, BorderLayout.WEST);
        item.add(textPanel, BorderLayout.CENTER);

        return item;
    }

    private JPanel createLoginCard() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);

        // Clean elevated white card
        JPanel card = new RoundedPanel(16, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(400, 480));
        card.setMaximumSize(new Dimension(420, 500));
        card.setBorder(BorderFactory.createEmptyBorder(32, 34, 30, 34));

        // Header
        JLabel h1 = new JLabel("Welcome back");
        h1.setFont(new Font("SansSerif", Font.BOLD, 22));
        h1.setForeground(ThemeColors.PRIMARY_TEXT);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to your ConnectAI account");
        sub.setFont(ThemeFonts.BODY_MEDIUM);
        sub.setForeground(ThemeColors.MUTED_TEXT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(h1);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(22));

        // Email Label & Input Field
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(ThemeFonts.BODY_BOLD);
        emailLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginEmailField = new RoundedTextField("name@work-email.com");
        loginEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(loginEmailField);
        card.add(Box.createVerticalStrut(14));

        // Password Label & Forgot Password Link
        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setOpaque(false);
        passLabelRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(ThemeFonts.BODY_BOLD);
        passLabel.setForeground(ThemeColors.SECONDARY_TEXT);

        JLabel forgotLabel = new JLabel("Forgot password?");
        forgotLabel.setFont(ThemeFonts.BODY_SMALL);
        forgotLabel.setForeground(ThemeColors.PRIMARY_ACCENT);
        forgotLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new ForgotPasswordDialog(AuthFrame.this).setVisible(true);
            }
        });

        passLabelRow.add(passLabel, BorderLayout.WEST);
        passLabelRow.add(forgotLabel, BorderLayout.EAST);

        loginPasswordField = new PasswordFieldWithToggle("Enter password");
        loginPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(passLabelRow);
        card.add(Box.createVerticalStrut(6));
        card.add(loginPasswordField);
        card.add(Box.createVerticalStrut(20));

        // Primary Action: Log In Button (Full width)
        loginButton = new PremiumButton("Log In", ThemeColors.PRIMARY_ACCENT, Color.WHITE);
        loginButton.setFont(ThemeFonts.BODY_BOLD);
        loginButton.setPreferredSize(new Dimension(332, 42));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        card.add(loginButton);
        card.add(Box.createVerticalStrut(20));

        // Switch to Sign Up
        JPanel switchBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchBox.setOpaque(false);
        switchBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel prompt = new JLabel("Don't have an account?");
        prompt.setFont(ThemeFonts.BODY_SMALL);
        prompt.setForeground(ThemeColors.MUTED_TEXT);

        JLabel switchRegister = new JLabel("Sign up");
        switchRegister.setFont(ThemeFonts.BODY_BOLD);
        switchRegister.setForeground(ThemeColors.PRIMARY_ACCENT);
        switchRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchRegister.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardsPanel, "REGISTER");
                getRootPane().setDefaultButton(registerButton);
            }
        });

        switchBox.add(prompt);
        switchBox.add(switchRegister);

        card.add(switchBox);

        container.add(card);
        return container;
    }

    private JPanel createRegisterCard() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);

        JPanel card = new RoundedPanel(16, ThemeColors.ELEVATED_SURFACE, ThemeColors.CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 560));
        card.setMaximumSize(new Dimension(440, 580));
        card.setBorder(BorderFactory.createEmptyBorder(28, 34, 26, 34));

        JLabel h1 = new JLabel("Create an Account");
        h1.setFont(new Font("SansSerif", Font.BOLD, 22));
        h1.setForeground(ThemeColors.PRIMARY_TEXT);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Get started with ConnectAI today");
        sub.setFont(ThemeFonts.BODY_MEDIUM);
        sub.setForeground(ThemeColors.MUTED_TEXT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(h1);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(18));

        regNameField = new RoundedTextField("Display Name (e.g. Alex Rivera)");
        regNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        regUserField = new RoundedTextField("Username (unique)");
        regUserField.setAlignmentX(Component.LEFT_ALIGNMENT);
        regEmailField = new RoundedTextField("Email address");
        regEmailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        regPasswordField = new PasswordFieldWithToggle("Password (min 6 chars)");
        regPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        regConfirmPasswordField = new PasswordFieldWithToggle("Confirm Password");
        regConfirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(createFieldBlock("Full Name", regNameField));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldBlock("Username", regUserField));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldBlock("Email Address", regEmailField));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldBlock("Password", regPasswordField));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldBlock("Confirm Password", regConfirmPasswordField));
        card.add(Box.createVerticalStrut(16));

        registerButton = new PremiumButton("Create Account", ThemeColors.PRIMARY_ACCENT, Color.WHITE);
        registerButton.setFont(ThemeFonts.BODY_BOLD);
        registerButton.setPreferredSize(new Dimension(352, 42));
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> performRegistration());

        card.add(registerButton);
        card.add(Box.createVerticalStrut(14));

        // Switch to Log In
        JPanel switchBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchBox.setOpaque(false);
        switchBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel prompt = new JLabel("Already have an account?");
        prompt.setFont(ThemeFonts.BODY_SMALL);
        prompt.setForeground(ThemeColors.MUTED_TEXT);

        JLabel switchLogin = new JLabel("Log in");
        switchLogin.setFont(ThemeFonts.BODY_BOLD);
        switchLogin.setForeground(ThemeColors.PRIMARY_ACCENT);
        switchLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardsPanel, "LOGIN");
                getRootPane().setDefaultButton(loginButton);
            }
        });

        switchBox.add(prompt);
        switchBox.add(switchLogin);
        card.add(switchBox);

        container.add(card);
        return container;
    }

    private JPanel createFieldBlock(String labelText, Component field) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l = new JLabel(labelText);
        l.setFont(ThemeFonts.BODY_SMALL);
        l.setForeground(ThemeColors.SECONDARY_TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(l);
        block.add(Box.createVerticalStrut(4));
        block.add(field);

        return block;
    }

    private void performLogin() {
        if (!loginButton.isEnabled()) return;
        AppConfig.setMockMode(false);
        if (!AppConfig.isSupabaseConfigured()) {
            ToastManager.showToast(this, "Please configure Supabase URL & Anon Key", ToastManager.ToastType.WARNING);
            showSupabaseConfigDialog();
            return;
        }

        String email = loginEmailField.getText().trim();
        String password = loginPasswordField.getPasswordString();

        if (email.isBlank() || password.isBlank()) {
            ToastManager.showToast(this, "Please fill in email and password", ToastManager.ToastType.WARNING);
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AuthService.getInstance().signIn(email, password).get();
                return null;
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Log In");
                try {
                    get();
                    ToastManager.showToast(AuthFrame.this, "Welcome back!", ToastManager.ToastType.SUCCESS);
                    dispose();
                    SwingUtilities.invokeLater(() -> new MainChatFrame().setVisible(true));
                } catch (Exception ex) {
                    String msg = extractErrorMessage(ex);
                    // Provide a friendly message for email confirmation requirement
                    if (msg.toLowerCase().contains("email not confirmed") || msg.toLowerCase().contains("email_not_confirmed")) {
                        msg = "Please check your email inbox and confirm your account before logging in.";
                    }
                    ToastManager.showToast(AuthFrame.this, "Login failed: " + msg, ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }

    private void performRegistration() {
        if (!registerButton.isEnabled()) return;
        AppConfig.setMockMode(false);
        if (!AppConfig.isSupabaseConfigured()) {
            ToastManager.showToast(this, "Please configure Supabase URL & Anon Key", ToastManager.ToastType.WARNING);
            showSupabaseConfigDialog();
            return;
        }

        String displayName = regNameField.getText().trim();
        String username = regUserField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = regPasswordField.getPasswordString();
        String confirm = regConfirmPasswordField.getPasswordString();

        if (displayName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            ToastManager.showToast(this, "Please fill in all required fields", ToastManager.ToastType.WARNING);
            return;
        }

        if (!password.equals(confirm)) {
            ToastManager.showToast(this, "Passwords do not match", ToastManager.ToastType.ERROR);
            return;
        }

        if (password.length() < 6) {
            ToastManager.showToast(this, "Password must be at least 6 characters", ToastManager.ToastType.WARNING);
            return;
        }

        registerButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AuthService.getInstance().signUp(email, password, username, displayName, null).get();
                return null;
            }

            @Override
            protected void done() {
                registerButton.setEnabled(true);
                try {
                    get();
                    if (AuthService.getInstance().getCurrentUser() != null) {
                        dispose();
                        new MainChatFrame().setVisible(true);
                        return;
                    }
                    loginEmailField.setText(email);
                    // Show confirmation email notice instead of going directly to chat
                    ToastManager.showToast(AuthFrame.this,
                            "Account created! Please check your email to confirm your account.",
                            ToastManager.ToastType.SUCCESS);
                    // Switch back to login screen so user can log in after confirmation
                    cardLayout.show(cardsPanel, "LOGIN");
                getRootPane().setDefaultButton(loginButton);
                } catch (Exception ex) {
                    String msg = extractErrorMessage(ex);
                    if (msg.toLowerCase().contains("already registered") || msg.toLowerCase().contains("user already exists")) {
                        msg = "An account with this email already exists. Please log in instead.";
                    }
                    ToastManager.showToast(AuthFrame.this, "Sign up failed: " + msg, ToastManager.ToastType.ERROR);
                }
            }
        }.execute();
    }

    /**
     * Safely extracts a clean, user-friendly error message from an exception chain.
     * Recursively unwraps causes and strips all Java class name prefixes.
     */
    private String extractErrorMessage(Exception ex) {
        if (ex == null) return "An error occurred. Please try again.";

        // Unwrap the full cause chain to find the deepest meaningful message
        Throwable t = ex;
        String bestMsg = null;
        while (t != null) {
            if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException)
                return "Could not reach Supabase. Check the project URL and internet connection.";
            if (t instanceof java.net.http.HttpTimeoutException)
                return "The connection timed out. Please try again.";
            String msg = t.getMessage();
            if (msg != null && !msg.isBlank()) {
                bestMsg = msg;
            }
            t = t.getCause();
        }

        if (bestMsg == null) return "An error occurred. Please try again.";

        // Strip common Java exception class prefixes
        for (String prefix : new String[]{
                "java.lang.RuntimeException: ",
                "java.util.concurrent.ExecutionException: ",
                "java.io.IOException: "
        }) {
            if (bestMsg.startsWith(prefix)) {
                bestMsg = bestMsg.substring(prefix.length());
            }
        }

        // Map common Supabase error messages to friendly text
        String lower = bestMsg.toLowerCase();
        if (lower.contains("over_email_send_rate_limit") || lower.contains("email rate limit")) {
            return "Too many signup attempts. Please wait a few minutes and try again, or disable email confirmation in your Supabase dashboard.";
        }
        if (lower.contains("email_not_confirmed") || lower.contains("email not confirmed")) {
            return "Please check your inbox and confirm your email before logging in.";
        }
        if (lower.contains("invalid_credentials") || lower.contains("invalid login credentials")) {
            return "Email or password was not accepted. Check your email address, or use Forgot password to reset it.";
        }
        if (lower.contains("user already registered") || lower.contains("already registered")) {
            return "This email is already registered. Please log in instead.";
        }
        if (lower.contains("password should be at least")) {
            return "Password must be at least 6 characters.";
        }

        return bestMsg;
    }

    public void showSupabaseConfigDialog() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Connect to Supabase", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeColors.ELEVATED_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Supabase Backend Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(ThemeColors.PRIMARY_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><body style='width: 380px; color: #64748B; font-size: 11px;'>"
                + "Enter your Supabase Project URL and Public Anon Key. You can find both in your Supabase Dashboard under <b>Project Settings &gt; API</b>."
                + "</body></html>");
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel urlLabel = new JLabel("Supabase Project URL");
        urlLabel.setFont(ThemeFonts.BODY_BOLD);
        urlLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedTextField urlField = new RoundedTextField("https://your-project.supabase.co");
        urlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        String currentUrl = AppConfig.getSupabaseUrl();
        if (!currentUrl.contains("xyzcompany")) {
            urlField.setText(currentUrl);
        }

        JLabel keyLabel = new JLabel("Supabase Anon / Public Key");
        keyLabel.setFont(ThemeFonts.BODY_BOLD);
        keyLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedTextField keyField = new RoundedTextField("eyJhbGciOi...");
        keyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        String currentKey = AppConfig.getSupabaseAnonKey();
        if (!currentKey.contains("example_anon_key")) {
            keyField.setText(currentKey);
        }

        PremiumButton saveBtn = new PremiumButton("Save & Connect to Supabase", ThemeColors.PRIMARY_ACCENT, Color.WHITE);
        saveBtn.setFont(ThemeFonts.BODY_BOLD);
        saveBtn.setPreferredSize(new Dimension(380, 40));
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.addActionListener(ev -> {
            String u = urlField.getText().trim();
            String k = keyField.getText().trim();
            if (u.isBlank() || k.isBlank() || u.contains("your-project") || k.contains("eyJhbGciOi...")) {
                ToastManager.showToast(this, "Please enter valid Supabase URL and Anon Key", ToastManager.ToastType.WARNING);
                return;
            }
            try {
                AppConfig.validateSupabaseCredentials(u, k);
            } catch (IllegalArgumentException ex) {
                ToastManager.showToast(this, ex.getMessage(), ToastManager.ToastType.WARNING);
                return;
            }
            saveBtn.setEnabled(false);
            saveBtn.setText("Checking connection…");
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    com.connectai.util.HttpClientUtil.checkSupabaseConnection(u, k).get();
                    AppConfig.updateSupabaseCredentials(u, k);
                    return null;
                }
                protected void done() {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save & connect");
                    try {
                        get();
                        AuthService.getInstance().logout();
                        dialog.dispose();
                        ToastManager.showToast(AuthFrame.this, "Connection verified. You can sign in now.", ToastManager.ToastType.SUCCESS);
                    } catch (Exception ex) {
                        ToastManager.showToast(dialog, extractErrorMessage(ex), ToastManager.ToastType.ERROR);
                    }
                }
            }.execute();

        });

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(18));
        panel.add(urlLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(urlField);
        panel.add(Box.createVerticalStrut(12));
        panel.add(keyLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(keyField);
        panel.add(Box.createVerticalStrut(22));
        panel.add(saveBtn);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}
