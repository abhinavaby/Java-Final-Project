package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Conversation;
import com.connectai.model.ConversationMember;
import com.connectai.model.ConversationType;
import com.connectai.service.AuthService;
import com.connectai.service.ChatService;
import com.connectai.ui.components.AvatarComponent;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.RoundedTextField;
import com.connectai.ui.components.ToastManager;
import com.connectai.ui.group.CreateGroupDialog;
import com.connectai.ui.group.JoinGroupDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Clean, simple, and clutter-free conversation sidebar.
 */
public class ConversationListPanel extends JPanel {
    private Consumer<Conversation> onConversationSelected;
    private List<Conversation> allConversations = new ArrayList<>();
    private List<Conversation> filteredConversations = new ArrayList<>();
    private String selectedConvId = "conv-ai-bot";
    private String currentFilter = "ALL"; // ALL, DIRECT, GROUP
    private String searchQuery = "";

    private JPanel listContainer;
    private RoundedTextField searchField;
    private JLabel tabAll;
    private JLabel tabDirect;
    private JLabel tabGroup;

    public ConversationListPanel(Consumer<Conversation> onConversationSelected) {
        this.onConversationSelected = onConversationSelected;
        setLayout(new BorderLayout());
        setBackground(ThemeColors.SIDEBAR_BG);
        setPreferredSize(new Dimension(280, 800));
        setMinimumSize(new Dimension(260, 600));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeColors.DIVIDER));

        // 1. Top Header & Search Bar
        add(createTopSection(), BorderLayout.NORTH);

        // 2. Conversation Items Scroll View
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        listContainer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(scroll, BorderLayout.CENTER);

        // 3. User Profile Footer
        add(createUserFooter(), BorderLayout.SOUTH);

        refreshConversations();
    }

    private JPanel createTopSection() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        // Row 1: Logo & New Chat Action Button
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel logoBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoBox.setOpaque(false);

        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        logoIcon.setForeground(ThemeColors.PRIMARY_ACCENT);

        JLabel logoText = new JLabel("ConnectAI");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 18));
        logoText.setForeground(ThemeColors.PRIMARY_TEXT);

        logoBox.add(logoIcon);
        logoBox.add(logoText);

        IconButton newChatBtn = new IconButton("+", "Create New Group or Chat");
        newChatBtn.setFont(new Font("SansSerif", Font.BOLD, 19));
        newChatBtn.setForeground(ThemeColors.PRIMARY_ACCENT);
        newChatBtn.setPreferredSize(new Dimension(30, 30));
        newChatBtn.addActionListener(e -> {
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            new CreateGroupDialog(frame, this::refreshConversations).setVisible(true);
        });

        headerRow.add(logoBox, BorderLayout.WEST);
        headerRow.add(newChatBtn, BorderLayout.EAST);

        // Row 2: Search Input
        searchField = new RoundedTextField("Search conversations...");
        searchField.setPreferredSize(new Dimension(248, 34));
        searchField.setMaximumSize(new Dimension(280, 34));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchQuery = searchField.getText().trim().toLowerCase();
                applyFilter();
            }
        });

        // Row 3: Filter Tabs (All / Direct / Groups)
        JPanel tabsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabsRow.setOpaque(false);
        tabsRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        tabAll = createTabPill("All", "ALL");
        tabDirect = createTabPill("Direct", "DIRECT");
        tabGroup = createTabPill("Groups", "GROUP");

        tabsRow.add(tabAll);
        tabsRow.add(tabDirect);
        tabsRow.add(tabGroup);

        top.add(headerRow);
        top.add(Box.createVerticalStrut(12));
        top.add(searchField);
        top.add(Box.createVerticalStrut(10));
        top.add(tabsRow);
        top.add(Box.createVerticalStrut(6));

        return top;
    }

    private JLabel createTabPill(String label, String filterKey) {
        JLabel pill = new JLabel(label);
        pill.setFont(ThemeFonts.BODY_SMALL);
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        pill.setOpaque(true);

        updateTabAppearance(pill, filterKey.equals(currentFilter));

        pill.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                currentFilter = filterKey;
                updateTabAppearance(tabAll, "ALL".equals(currentFilter));
                updateTabAppearance(tabDirect, "DIRECT".equals(currentFilter));
                updateTabAppearance(tabGroup, "GROUP".equals(currentFilter));
                applyFilter();
            }
        });
        return pill;
    }

    private void updateTabAppearance(JLabel pill, boolean isSelected) {
        if (isSelected) {
            pill.setBackground(ThemeColors.PRIMARY_ACCENT_LIGHT);
            pill.setForeground(ThemeColors.PRIMARY_ACCENT);
        } else {
            pill.setBackground(ThemeColors.SIDEBAR_BG);
            pill.setForeground(ThemeColors.SECONDARY_TEXT);
        }
        pill.repaint();
    }

    private AvatarComponent userAvatar;
    private JLabel userNameLabel;

    private JPanel createUserFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(280, 58));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.DIVIDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userBox.setOpaque(false);

        String initialName = getEffectiveCurrentUserName();
        userAvatar = new AvatarComponent(36, initialName, true);
        userAvatar.setCustomColors(ThemeColors.AVATAR_DARK_BG, ThemeColors.AVATAR_DARK_FG);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        userNameLabel = new JLabel(initialName);
        userNameLabel.setFont(ThemeFonts.BODY_BOLD);
        userNameLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        JLabel role = new JLabel("● Online");
        role.setFont(ThemeFonts.CAPTION);
        role.setForeground(ThemeColors.SUCCESS_ONLINE);

        info.add(userNameLabel);
        info.add(role);

        userBox.add(userAvatar);
        userBox.add(info);

        IconButton settingsBtn = new IconButton("⚙", "Settings");
        settingsBtn.setPreferredSize(new Dimension(32, 32));
        settingsBtn.addActionListener(e -> {
            Frame f = (Frame) SwingUtilities.getWindowAncestor(this);
            new com.connectai.ui.components.SettingsDialog(f).setVisible(true);
        });

        footer.add(userBox, BorderLayout.WEST);
        footer.add(settingsBtn, BorderLayout.EAST);
        return footer;
    }

    public void updateCurrentUserDisplay() {
        String name = getEffectiveCurrentUserName();
        if (userNameLabel != null) {
            userNameLabel.setText(name);
        }
        if (userAvatar != null) {
            userAvatar.setDisplayName(name);
            userAvatar.repaint();
        }
    }

    private String getEffectiveCurrentUserName() {
        com.connectai.model.Profile p = AuthService.getInstance().getCurrentProfile();
        if (p != null && p.getDisplayName() != null && !p.getDisplayName().isBlank()) {
            return p.getDisplayName();
        }
        com.connectai.model.User u = AuthService.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
            String prefix = u.getEmail().split("@")[0];
            return Character.toUpperCase(prefix.charAt(0)) + (prefix.length() > 1 ? prefix.substring(1) : "");
        }
        return "You";
    }

    private void applyFilter() {
        filteredConversations.clear();
        for (Conversation c : allConversations) {
            // Filter by tab
            if ("DIRECT".equals(currentFilter) && c.getType() != ConversationType.DIRECT) continue;
            if ("GROUP".equals(currentFilter) && c.getType() != ConversationType.GROUP) continue;

            // Filter by search text
            if (!searchQuery.isEmpty()) {
                String title = c.getDisplayTitle() != null ? c.getDisplayTitle().toLowerCase() : "";
                String lastMsg = c.getLatestMessage() != null && c.getLatestMessage().getContent() != null
                        ? c.getLatestMessage().getContent().toLowerCase() : "";
                if (!title.contains(searchQuery) && !lastMsg.contains(searchQuery)) {
                    continue;
                }
            }
            filteredConversations.add(c);
        }
        renderConversationList();
    }

    private void renderConversationList() {
        listContainer.removeAll();

        if (filteredConversations.isEmpty()) {
            JLabel empty = new JLabel("No conversations found");
            empty.setFont(ThemeFonts.CAPTION);
            empty.setForeground(ThemeColors.MUTED_TEXT);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 0));
            listContainer.add(empty);
        } else {
            for (Conversation c : filteredConversations) {
                listContainer.add(createConversationRow(c));
                listContainer.add(Box.createVerticalStrut(4));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createConversationRow(Conversation c) {
        boolean isSelected = c.getId().equals(selectedConvId);
        JPanel row = new RoundedPanel(10, isSelected ? ThemeColors.PRIMARY_ACCENT_LIGHT : ThemeColors.SIDEBAR_BG);
        row.setLayout(new BorderLayout(10, 0));
        row.setPreferredSize(new Dimension(256, 52));
        row.setMaximumSize(new Dimension(270, 52));
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Left Avatar
        AvatarComponent av = new AvatarComponent(38, c.getDisplayTitle(), c.getType() == ConversationType.DIRECT);
        row.add(av, BorderLayout.WEST);

        // Center Title & Snippet
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel title = new JLabel(c.getDisplayTitle());
        title.setFont(isSelected ? ThemeFonts.BODY_BOLD : ThemeFonts.BODY_MEDIUM);
        title.setForeground(isSelected ? ThemeColors.PRIMARY_ACCENT : ThemeColors.PRIMARY_TEXT);

        String snippet = "Tap to chat";
        if (c.getLatestMessage() != null && c.getLatestMessage().getContent() != null) {
            snippet = c.getLatestMessage().getContent().replace("\n", " ");
            if (snippet.length() > 24) {
                snippet = snippet.substring(0, 22) + "…";
            }
        }
        JLabel sub = new JLabel(snippet);
        sub.setFont(ThemeFonts.CAPTION);
        sub.setForeground(ThemeColors.MUTED_TEXT);

        center.add(title);
        center.add(Box.createVerticalStrut(2));
        center.add(sub);
        row.add(center, BorderLayout.CENTER);

        // Right Unread Badge (if applicable)
        if ("conv-tech-team".equals(c.getId())) {
            JLabel badge = new JLabel(" 3 ");
            badge.setFont(ThemeFonts.BADGE);
            badge.setForeground(Color.WHITE);
            badge.setBackground(ThemeColors.BADGE_RED);
            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            JPanel badgeBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
            badgeBox.setOpaque(false);
            badgeBox.add(badge);
            row.add(badgeBox, BorderLayout.EAST);
        }

        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedConvId = c.getId();
                renderConversationList();
                if (onConversationSelected != null) {
                    onConversationSelected.accept(c);
                }
            }
        });

        return row;
    }

    public void refreshConversations() {
        updateCurrentUserDisplay();
        ChatService.getInstance().loadUserConversations().thenAccept(memberships -> {
            SwingUtilities.invokeLater(() -> {
                updateCurrentUserDisplay();
                allConversations.clear();
                for (ConversationMember cm : memberships) {
                    if (cm.getConversation() != null) {
                        allConversations.add(cm.getConversation());
                    }
                }
                applyFilter();
                // Select active conversation
                for (Conversation c : allConversations) {
                    if (c.getId().equals(selectedConvId)) {
                        if (onConversationSelected != null) onConversationSelected.accept(c);
                        return;
                    }
                }
                if (!allConversations.isEmpty() && onConversationSelected != null) {
                    selectedConvId = allConversations.get(0).getId();
                    onConversationSelected.accept(allConversations.get(0));
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                updateCurrentUserDisplay();
                System.err.println("Failed to load user conversations: " + ex.getMessage());
            });
            return null;
        });
    }
}
