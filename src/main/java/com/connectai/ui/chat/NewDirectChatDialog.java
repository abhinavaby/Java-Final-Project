package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Conversation;
import com.connectai.model.Profile;
import com.connectai.service.ChatService;
import com.connectai.ui.components.PremiumButton;
import com.connectai.ui.components.RoundedTextField;
import com.connectai.ui.components.ToastManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Find a ConnectAI user by username and start a direct conversation. */
public class NewDirectChatDialog extends JDialog {
    private final Consumer<Conversation> onConversationCreated;
    private final RoundedTextField searchField = new RoundedTextField("Search by username...");
    private final DefaultListModel<Profile> resultsModel = new DefaultListModel<>();
    private final JList<Profile> results = new JList<>(resultsModel);
    private final JLabel status = new JLabel("Enter at least 2 characters to search.");
    private final javax.swing.Timer searchTimer;
    private long searchGeneration;

    public NewDirectChatDialog(Frame owner, Consumer<Conversation> onConversationCreated) {
        super(owner, "New direct conversation", true);
        this.onConversationCreated = onConversationCreated;
        searchTimer = new javax.swing.Timer(300, e -> search());
        searchTimer.setRepeats(false);
        setSize(420, 480);
        setLocationRelativeTo(owner);

        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(ThemeColors.MAIN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel("Find a person by name or username");
        heading.setFont(ThemeFonts.TITLE_SMALL);
        heading.setForeground(ThemeColors.PRIMARY_TEXT);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(searchField, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        results.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                Profile profile = (Profile) value;
                String username = profile.getUsername() == null ? "" : "  @" + profile.getUsername();
                return super.getListCellRendererComponent(list, profile.getEffectiveName() + username,
                        index, selected, focus);
            }
        });
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeColors.CARD_BORDER));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        status.setFont(ThemeFonts.CAPTION);
        status.setForeground(ThemeColors.MUTED_TEXT);
        bottom.add(status, BorderLayout.CENTER);
        PremiumButton start = new PremiumButton("Start chat", ThemeColors.PRIMARY_ACCENT, java.awt.Color.WHITE);
        start.addActionListener(e -> startChat());
        bottom.add(start, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        searchField.addActionListener(e -> search());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() { searchTimer.restart(); }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        });
        results.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) startChat(); }
        });
        setContentPane(panel);
    }

    private void search() {
        String query = searchField.getText().trim();
        long generation = ++searchGeneration;
        resultsModel.clear();
        if (query.length() < 2) {
            status.setText("Enter at least 2 characters to search.");
            return;
        }
        status.setText("Searching names and usernames…");
        ChatService.getInstance().searchProfilesByUsername(query).whenComplete((profiles, error) ->
                SwingUtilities.invokeLater(() -> {
                    if (generation != searchGeneration || !isDisplayable()) return;
                    if (error != null) {
                        status.setText(com.connectai.util.ErrorMessages.from(error));
                        return;
                    }
                    List<Profile> matches = profiles == null ? List.of() : profiles;
                    matches.forEach(resultsModel::addElement);
                    status.setText(matches.isEmpty() ? "No users found." : matches.size() + " user(s) found.");
                }));
    }

    private void startChat() {
        Profile selected = results.getSelectedValue();
        if (selected == null) {
            ToastManager.showToast(this, "Choose a person from the results first.", ToastManager.ToastType.WARNING);
            return;
        }
        status.setText("Starting conversation…");
        ChatService.getInstance().startDirectChat(selected.getId()).whenComplete((conversation, error) ->
                SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        status.setText(com.connectai.util.ErrorMessages.from(error));
                        return;
                    }
                    if (onConversationCreated != null) onConversationCreated.accept(conversation);
                    dispose();
                }));
    }
}
