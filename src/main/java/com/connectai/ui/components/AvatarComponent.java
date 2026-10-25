package com.connectai.ui.components;

import com.connectai.config.ThemeColors;
import com.connectai.config.ThemeFonts;
import com.connectai.util.ImageUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class AvatarComponent extends JComponent {
    private int size;
    private String displayName;
    private BufferedImage image;
    private boolean isOnline = true;
    private boolean showOnlineStatus;
    private Color customBgColor;
    private Color customFgColor;
    private int ringWidth = 0;
    private Color ringColor = Color.WHITE;
    private String loadedAvatarPath;

    public AvatarComponent(int size, String displayName, boolean showOnlineStatus) {
        this.size = size;
        this.displayName = displayName;
        this.showOnlineStatus = showOnlineStatus;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    public AvatarComponent(int size, String displayName, boolean showOnlineStatus, Color bg, Color fg) {
        this(size, displayName, showOnlineStatus);
        this.customBgColor = bg;
        this.customFgColor = fg;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        repaint();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void loadAvatar(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || storagePath.equals(loadedAvatarPath)) return;
        loadedAvatarPath = storagePath;
        var auth = com.connectai.service.AuthService.getInstance();
        new com.connectai.repository.StorageRepository().downloadAvatar(storagePath, auth.getAccessToken())
                .thenAccept(bytes -> {
                    try {
                        BufferedImage loaded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                        if (loaded != null) javax.swing.SwingUtilities.invokeLater(() -> setImage(loaded));
                    } catch (Exception ignored) {}
                });
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
        repaint();
    }

    public void setCustomColors(Color bg, Color fg) {
        this.customBgColor = bg;
        this.customFgColor = fg;
        repaint();
    }

    public void setRing(int width, Color color) {
        this.ringWidth = width;
        this.ringColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int actualSize = size;
        int offset = 0;
        if (ringWidth > 0) {
            actualSize = size - (ringWidth * 2);
            offset = ringWidth;
        }

        if (image != null) {
            BufferedImage circular = ImageUtil.makeCircularImage(image, actualSize);
            g2.drawImage(circular, offset, offset, null);
        } else {
            // Pick background and text color
            Color bg = customBgColor != null ? customBgColor : getPaletteBg(displayName);
            Color fg = customFgColor != null ? customFgColor : getPaletteFg(displayName);

            g2.setColor(bg);
            g2.fillOval(offset, offset, actualSize, actualSize);

            g2.setColor(fg);
            int fontSize = Math.max(10, (int) (actualSize * 0.40));
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));

            String initials = getInitials(displayName);
            int textX = offset + (actualSize - g2.getFontMetrics().stringWidth(initials)) / 2;
            int textY = offset + (actualSize - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(initials, textX, textY);
        }

        // Optional outer ring (e.g. 3px white ring on banner avatar)
        if (ringWidth > 0) {
            g2.setColor(ringColor);
            g2.setStroke(new java.awt.BasicStroke(ringWidth));
            int half = ringWidth / 2;
            g2.drawOval(half, half, size - ringWidth, size - ringWidth);
        }

        // Draw online indicator dot
        if (showOnlineStatus) {
            int dotSize = Math.max(8, size / 4);
            int dotX = size - dotSize - 1;
            int dotY = size - dotSize - 1;

            // White border ring around online dot
            g2.setColor(Color.WHITE);
            g2.fillOval(dotX - 2, dotY - 2, dotSize + 4, dotSize + 4);

            g2.setColor(isOnline ? ThemeColors.SUCCESS_ONLINE : ThemeColors.MUTED_TEXT);
            g2.fillOval(dotX, dotY, dotSize, dotSize);
        }

        g2.dispose();
    }

    public static String getInitials(String name) {
        if (name == null || name.isBlank()) return "CA";
        String clean = name.replace("🤖", "").replace("⚡", "").replace("🎨", "").replace("🛠️", "")
                .replace("(You)", "").trim();
        if (clean.isBlank()) return "CA";

        String[] parts = clean.split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0].substring(0, 1);
            String second = parts[1].substring(0, 1);
            if (parts[1].equals("&") && parts.length >= 3) {
                return (first + "&").toUpperCase();
            }
            return (first + second).toUpperCase();
        }
        return clean.substring(0, Math.min(2, clean.length())).toUpperCase();
    }

    private static Color getPaletteBg(String name) {
        if (name == null) return ThemeColors.AVATAR_DARK_BG;
        String lower = name.toLowerCase();
        if (lower.contains("alex") || lower.contains("rivera")) return ThemeColors.AVATAR_DARK_BG;
        if (lower.contains("connectai") || lower.contains("assistant") || lower.contains("bot")) return ThemeColors.AVATAR_DARK_BG;
        if (lower.contains("tech") || lower.contains("t&")) return ThemeColors.AVATAR_TEAL_BG;
        if (lower.contains("design") || lower.contains("ds")) return ThemeColors.AVATAR_ROSE_BG;
        if (lower.contains("marketing") || lower.contains("mr")) return ThemeColors.AVATAR_PURPLE_BG;
        if (lower.contains("project") || lower.contains("pr") || lower.contains("r&d")) return ThemeColors.AVATAR_BLUE_BG;
        if (lower.contains("sarah") || lower.contains("jenkins") || lower.contains("sj")) return ThemeColors.AVATAR_SALMON_BG;
        if (lower.contains("david") || lower.contains("chen") || lower.contains("dc")) return ThemeColors.AVATAR_AMBER_BG;
        if (lower.contains("elena") || lower.contains("rostova") || lower.contains("er")) return ThemeColors.AVATAR_MINT_BG;
        if (lower.contains("marcus") || lower.contains("vance") || lower.contains("mv")) return ThemeColors.AVATAR_GOLD_BG;

        // Deterministic hash selection for any other name
        int hash = Math.abs(name.hashCode()) % 6;
        switch (hash) {
            case 0: return ThemeColors.AVATAR_TEAL_BG;
            case 1: return ThemeColors.AVATAR_ROSE_BG;
            case 2: return ThemeColors.AVATAR_PURPLE_BG;
            case 3: return ThemeColors.AVATAR_BLUE_BG;
            case 4: return ThemeColors.AVATAR_SALMON_BG;
            default: return ThemeColors.AVATAR_AMBER_BG;
        }
    }

    private static Color getPaletteFg(String name) {
        if (name == null) return ThemeColors.AVATAR_DARK_FG;
        String lower = name.toLowerCase();
        if (lower.contains("alex") || lower.contains("rivera")) return ThemeColors.AVATAR_DARK_FG;
        if (lower.contains("connectai") || lower.contains("assistant") || lower.contains("bot")) return ThemeColors.AVATAR_DARK_FG;
        if (lower.contains("tech") || lower.contains("t&")) return ThemeColors.AVATAR_TEAL_FG;
        if (lower.contains("design") || lower.contains("ds")) return ThemeColors.AVATAR_ROSE_FG;
        if (lower.contains("marketing") || lower.contains("mr")) return ThemeColors.AVATAR_PURPLE_FG;
        if (lower.contains("project") || lower.contains("pr") || lower.contains("r&d")) return ThemeColors.AVATAR_BLUE_FG;
        if (lower.contains("sarah") || lower.contains("jenkins") || lower.contains("sj")) return ThemeColors.AVATAR_SALMON_FG;
        if (lower.contains("david") || lower.contains("chen") || lower.contains("dc")) return ThemeColors.AVATAR_AMBER_FG;
        if (lower.contains("elena") || lower.contains("rostova") || lower.contains("er")) return ThemeColors.AVATAR_MINT_FG;
        if (lower.contains("marcus") || lower.contains("vance") || lower.contains("mv")) return ThemeColors.AVATAR_GOLD_FG;

        int hash = Math.abs(name.hashCode()) % 6;
        switch (hash) {
            case 0: return ThemeColors.AVATAR_TEAL_FG;
            case 1: return ThemeColors.AVATAR_ROSE_FG;
            case 2: return ThemeColors.AVATAR_PURPLE_FG;
            case 3: return ThemeColors.AVATAR_BLUE_FG;
            case 4: return ThemeColors.AVATAR_SALMON_FG;
            default: return ThemeColors.AVATAR_AMBER_FG;
        }
    }
}
