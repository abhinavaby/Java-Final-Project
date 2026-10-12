package com.connectai.ui.chat;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.model.Conversation;
import com.connectai.model.ConversationMember;
import com.connectai.model.Role;
import com.connectai.ui.components.AvatarComponent;
import com.connectai.ui.components.IconButton;
import com.connectai.ui.components.RoundedPanel;
import com.connectai.ui.components.ToastManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;

/**
 * Clean, uncluttered conversation details side panel.
 */
public class RightInfoPanel extends JPanel {
    private Conversation conversation;
    private AvatarComponent profileAvatar;
    private JLabel nameLabel;
    private JLabel descLabel;
    private JLabel inviteCodeLabel;
    private JPanel membersListContainer;
    private JLabel memberCountBadge;
    private Runnable onCloseCallback;
    private Runnable onDeletedCallback;
    private javax.swing.JButton deleteGroupButton;

    public RightInfoPanel(Runnable onCloseCallback, Runnable onDeletedCallback) {
        this.onDeletedCallback = onDeletedCallback;
        this.onCloseCallback = onCloseCallback;
        setLayout(new BorderLayout());
        setBackground(ThemeColors.SIDEBAR_BG);
        setPreferredSize(new Dimension(300, 800));
        setMinimumSize(new Dimension(280, 500));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ThemeColors.DIVIDER));

        // Top Header with Close Button
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        headerBar.setPreferredSize(new Dimension(300, 52));
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.DIVIDER),
                BorderFactory.createEmptyBorder(8, 16, 8, 12)
        ));

        JLabel headerTitle = new JLabel("Details");
        headerTitle.setFont(ThemeFonts.TITLE_SMALL);
        headerTitle.setForeground(ThemeColors.PRIMARY_TEXT);

        IconButton closeBtn = new IconButton("✕", "Close details panel");
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.addActionListener(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });

        headerBar.add(headerTitle, BorderLayout.WEST);
        headerBar.add(closeBtn, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // Body Content
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));

        // Profile Identity Card
        profileAvatar = new AvatarComponent(58, "ConnectAI Assistant 🤖", true);
        profileAvatar.setAlignmentX(CENTER_ALIGNMENT);

        nameLabel = new JLabel("Tech & AI Product Team", SwingConstants.CENTER);
        nameLabel.setFont(ThemeFonts.TITLE_MEDIUM);
        nameLabel.setForeground(ThemeColors.PRIMARY_TEXT);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);

        descLabel = new JLabel("Core architecture & Edge Functions", SwingConstants.CENTER);
        descLabel.setFont(ThemeFonts.CAPTION);
        descLabel.setForeground(ThemeColors.MUTED_TEXT);
        descLabel.setAlignmentX(CENTER_ALIGNMENT);

        body.add(profileAvatar);
        body.add(Box.createVerticalStrut(10));
        body.add(nameLabel);
        body.add(Box.createVerticalStrut(4));
        body.add(descLabel);
        body.add(Box.createVerticalStrut(18));

        // Quick Actions
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(CENTER_ALIGNMENT);

        actionsRow.add(createActionButton("📞", "Call"));
        actionsRow.add(createActionButton("📹", "Video"));
        actionsRow.add(createActionButton("📋", "Invite"));

        body.add(actionsRow);
        body.add(Box.createVerticalStrut(22));

        // Members Section
        JPanel membersHeader = new JPanel(new BorderLayout());
        membersHeader.setOpaque(false);
        membersHeader.setMaximumSize(new Dimension(270, 26));

        JLabel mTitle = new JLabel("Members");
        mTitle.setFont(ThemeFonts.BODY_BOLD);
        mTitle.setForeground(ThemeColors.PRIMARY_TEXT);

        memberCountBadge = new JLabel("6 members");
        memberCountBadge.setFont(ThemeFonts.CAPTION);
        memberCountBadge.setForeground(ThemeColors.MUTED_TEXT);

        membersHeader.add(mTitle, BorderLayout.WEST);
        membersHeader.add(memberCountBadge, BorderLayout.EAST);
        membersHeader.setAlignmentX(CENTER_ALIGNMENT);

        body.add(membersHeader);
        body.add(Box.createVerticalStrut(8));

        membersListContainer = new JPanel();
        membersListContainer.setLayout(new BoxLayout(membersListContainer, BoxLayout.Y_AXIS));
        membersListContainer.setOpaque(false);
        membersListContainer.setAlignmentX(CENTER_ALIGNMENT);

        body.add(membersListContainer);
        body.add(Box.createVerticalStrut(18));

        // Group Code Card
        JPanel codeCard = new RoundedPanel(10, ThemeColors.INCOMING_BUBBLE, ThemeColors.CARD_BORDER);
        codeCard.setLayout(new BorderLayout(8, 0));
        codeCard.setPreferredSize(new Dimension(260, 42));
        codeCard.setMaximumSize(new Dimension(270, 42));
        codeCard.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
        codeCard.setAlignmentX(CENTER_ALIGNMENT);

        inviteCodeLabel = new JLabel("CONNECT2026");
        inviteCodeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        inviteCodeLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        IconButton copyBtn = new IconButton("📋", "Copy Code");
        copyBtn.setPreferredSize(new Dimension(28, 28));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(inviteCodeLabel.getText()), null);
            ToastManager.showToast(this, "Code copied: " + inviteCodeLabel.getText(), ToastManager.ToastType.SUCCESS);
        });

        codeCard.add(inviteCodeLabel, BorderLayout.CENTER);
        codeCard.add(copyBtn, BorderLayout.EAST);

        body.add(codeCard);
        body.add(Box.createVerticalStrut(16));
        deleteGroupButton = new javax.swing.JButton("Delete group");
        deleteGroupButton.setForeground(ThemeColors.ERROR);
        deleteGroupButton.setAlignmentX(CENTER_ALIGNMENT);
        deleteGroupButton.setVisible(false);
        deleteGroupButton.addActionListener(e -> deleteCurrentGroup());
        body.add(deleteGroupButton);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);

        loadDefaultMembers();
    }

    private JPanel createActionButton(String icon, String label) {
        JPanel btn = new RoundedPanel(14, Color.WHITE, ThemeColors.CARD_BORDER);
        btn.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        btn.setPreferredSize(new Dimension(78, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel l = new JLabel(icon + " " + label);
        l.setFont(ThemeFonts.BODY_SMALL);
        l.setForeground(ThemeColors.PRIMARY_TEXT);
        btn.add(l);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if ("Invite".equals(label)) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(inviteCodeLabel.getText()), null);
                    ToastManager.showToast(RightInfoPanel.this, "Invite code copied!", ToastManager.ToastType.SUCCESS);
                } else {
                    ToastManager.showToast(RightInfoPanel.this, label + " clicked", ToastManager.ToastType.INFO);
                }
            }
        });
        return btn;
    }

    private void loadDefaultMembers() {
        membersListContainer.removeAll();
        String currentName = "You";
        com.connectai.model.Profile p = com.connectai.service.AuthService.getInstance().getCurrentProfile();
        if (p != null && p.getDisplayName() != null && !p.getDisplayName().isBlank()) {
            currentName = p.getDisplayName();
        } else if (com.connectai.service.AuthService.getInstance().getCurrentUser() != null
                && com.connectai.service.AuthService.getInstance().getCurrentUser().getEmail() != null) {
            String prefix = com.connectai.service.AuthService.getInstance().getCurrentUser().getEmail().split("@")[0];
            currentName = Character.toUpperCase(prefix.charAt(0)) + (prefix.length() > 1 ? prefix.substring(1) : "");
        }
        addMemberRow(currentName + " (You)", true, "Member");
        memberCountBadge.setText("1 member");
        membersListContainer.revalidate();
        membersListContainer.repaint();
    }

    private void addMemberRow(String name, boolean online, String role) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(260, 32));
        row.setMaximumSize(new Dimension(270, 32));
        row.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

        AvatarComponent av = new AvatarComponent(24, name, false);

        JLabel nameLabel = new JLabel(name.length() > 17 ? name.substring(0, 15) + "…" : name);
        nameLabel.setFont(ThemeFonts.BODY_SMALL);
        nameLabel.setForeground(ThemeColors.PRIMARY_TEXT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(av);
        left.add(nameLabel);

        JLabel roleBadge = new JLabel(role);
        roleBadge.setFont(ThemeFonts.BADGE);
        if ("Owner".equals(role)) {
            roleBadge.setForeground(ThemeColors.PRIMARY_ACCENT);
            roleBadge.setBackground(ThemeColors.PRIMARY_ACCENT_LIGHT);
        } else {
            roleBadge.setForeground(ThemeColors.MUTED_TEXT);
            roleBadge.setBackground(ThemeColors.INCOMING_BUBBLE);
        }
        roleBadge.setOpaque(true);
        roleBadge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        row.add(left, BorderLayout.WEST);
        row.add(roleBadge, BorderLayout.EAST);

        membersListContainer.add(row);
        membersListContainer.add(Box.createVerticalStrut(2));
    }

    public void setConversation(Conversation conv) {
        this.conversation = conv;
        if (conv == null) return;

        deleteGroupButton.setVisible(conv.getType() == com.connectai.model.ConversationType.GROUP
                && com.connectai.service.AuthService.getInstance().getCurrentUser() != null
                && com.connectai.service.AuthService.getInstance().getCurrentUser().getId().equals(conv.getCreatedBy()));
        nameLabel.setText(conv.getDisplayTitle());
        profileAvatar.setDisplayName(conv.getDisplayTitle());

        if (conv.getInviteCode() != null && !conv.getInviteCode().isBlank()) {
            inviteCodeLabel.setText(conv.getInviteCode());
        }

        if (conv.getDescription() != null && !conv.getDescription().isBlank()) {
            descLabel.setText("<html><center style='width: 220px;'>" + conv.getDescription() + "</center></html>");
        } else {
            descLabel.setText("Conversation details");
        }

        // Fetch live group members
        com.connectai.service.ChatService.getInstance().getGroupMembers(conv.getId())
                .thenAccept(members -> {
                    SwingUtilities.invokeLater(() -> {
                        if (members != null && !members.isEmpty()) {
                            membersListContainer.removeAll();
                            memberCountBadge.setText(members.size() + (members.size() == 1 ? " member" : " members"));
                            for (ConversationMember cm : members) {
                                String mName = cm.getProfile() != null ? cm.getProfile().getEffectiveName() : "User";
                                String role = cm.getRole() == Role.OWNER ? "Owner" : (cm.getRole() == Role.ADMIN ? "Admin" : "Member");
                                boolean online = cm.getProfile() != null && cm.getProfile().isOnline();
                                addMemberRow(mName, online, role);
                            }
                            membersListContainer.revalidate();
                            membersListContainer.repaint();
                        } else {
                            loadDefaultMembers();
                        }
                    });
                }).exceptionally(ex -> {
                    SwingUtilities.invokeLater(this::loadDefaultMembers);
                    return null;
                });
    }
    private void deleteCurrentGroup() {
        if (conversation == null) return;
        Conversation target = conversation;
        if (javax.swing.JOptionPane.showConfirmDialog(this,
                "Delete this group and all its messages? This cannot be undone.", "Delete group",
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE)
                != javax.swing.JOptionPane.OK_OPTION) return;
        deleteGroupButton.setEnabled(false);
        com.connectai.service.ChatService.getInstance().deleteGroup(target).whenComplete((ignored, error) ->
                SwingUtilities.invokeLater(() -> {
                    deleteGroupButton.setEnabled(true);
                    if (error != null) ToastManager.showToast(this, com.connectai.util.ErrorMessages.from(error), ToastManager.ToastType.ERROR);
                    else if (onDeletedCallback != null) onDeletedCallback.run();
                }));
    }
}
