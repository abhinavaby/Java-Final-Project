package com.connectai.config;

import java.awt.Color;

/**
 * Modern Clean SaaS Theme (Vibrant Blue & Light Slate Palette matching reference design).
 */
public class ThemeColors {
    // Backgrounds
    public static Color MAIN_BG          = Color.decode("#F5F4F0"); // clean soft-slate window background
    public static Color SIDEBAR_BG       = Color.decode("#FFFFFF"); // clean white sidebar
    public static Color ELEVATED_SURFACE = Color.decode("#FFFFFF"); // card background
    public static Color MESSAGE_AREA     = Color.decode("#FAF9F6"); // center chat canvas

    // Accents
    public static Color PRIMARY_ACCENT       = Color.decode("#286454"); // vibrant royal blue (Brand Blue)
    public static Color PRIMARY_ACCENT_HOVER = Color.decode("#1D4F42");
    public static Color PRIMARY_ACCENT_LIGHT = Color.decode("#EAF2ED"); // light blue active tint
    public static Color SECONDARY_ACCENT     = Color.decode("#427965"); // secondary blue

    // Message Bubbles
    public static Color OUTGOING_BUBBLE  = Color.decode("#EAF2ED"); // solid vibrant blue
    public static Color INCOMING_BUBBLE  = Color.decode("#EEEFEA"); // soft slate gray bubble
    public static Color OUTGOING_TEXT    = Color.decode("#242B28"); // white text for outgoing
    public static Color INCOMING_TEXT    = Color.decode("#242B28"); // dark text for incoming

    // Text
    public static Color PRIMARY_TEXT   = Color.decode("#242B28"); // deep slate 900
    public static Color SECONDARY_TEXT = Color.decode("#59635D"); // slate 600
    public static Color MUTED_TEXT     = Color.decode("#768078"); // slate 400
    public static Color ACCENT_TEXT    = Color.decode("#FFFFFF"); // white text on blue buttons

    // Status & Semantic
    public static Color SUCCESS_ONLINE = Color.decode("#10B981"); // emerald green online dot
    public static Color WARNING        = Color.decode("#F59E0B"); // amber gold
    public static Color ERROR          = Color.decode("#EF4444"); // bright coral red
    public static Color DIVIDER        = Color.decode("#E5E7E1"); // subtle crisp border/divider
    public static Color CARD_BORDER    = Color.decode("#DFE3DB"); // soft component border

    // Interactive States & Pills
    public static Color HOVER_OVERLAY     = new Color(40, 100, 84, 14);  // subtle blue hover
    public static Color SELECTION_OVERLAY = Color.decode("#EAF2ED");      // light blue selection
    public static Color DATE_PILL_BG      = Color.decode("#DFE3DB");      // "Today" date badge
    public static Color BADGE_RED         = Color.decode("#EF4444");      // unread count badge
    public static Color BADGE_BLUE        = Color.decode("#286454");      // blue pill badge
    public static Color SPARKLE_GOLD      = Color.decode("#F59E0B");      // Catch Me Up sparkle

    // Avatar Color Palette (Pastels matching reference UI)
    public static Color AVATAR_DARK_BG   = Color.decode("#334155"); // Alex / ConnectAI dark slate
    public static Color AVATAR_DARK_FG   = Color.decode("#FFFFFF");
    public static Color AVATAR_TEAL_BG   = Color.decode("#99F6E4"); // Tech & AI Team
    public static Color AVATAR_TEAL_FG   = Color.decode("#0F766E");
    public static Color AVATAR_ROSE_BG   = Color.decode("#FECDD3"); // Design Guild
    public static Color AVATAR_ROSE_FG   = Color.decode("#BE123C");
    public static Color AVATAR_PURPLE_BG = Color.decode("#DDD6FE"); // Marketing Team
    public static Color AVATAR_PURPLE_FG = Color.decode("#6D28D9");
    public static Color AVATAR_BLUE_BG   = Color.decode("#BFDBFE"); // Project R&D
    public static Color AVATAR_BLUE_FG   = Color.decode("#1D4F42");
    public static Color AVATAR_SALMON_BG = Color.decode("#FDA4AF"); // Sarah Jenkins
    public static Color AVATAR_SALMON_FG = Color.decode("#9F1239");
    public static Color AVATAR_AMBER_BG  = Color.decode("#FDE68A"); // David Chen
    public static Color AVATAR_AMBER_FG  = Color.decode("#92400E");
    public static Color AVATAR_MINT_BG   = Color.decode("#A7F3D0"); // Elena Rostova
    public static Color AVATAR_MINT_FG   = Color.decode("#065F46");
    public static Color AVATAR_GOLD_BG   = Color.decode("#FEF08A"); // Marcus Vance
    public static Color AVATAR_GOLD_FG   = Color.decode("#854D0E");

    public static void applyDarkMode(boolean dark) {
        if (dark) {
            MAIN_BG = Color.decode("#111315"); SIDEBAR_BG = Color.decode("#17191B");
            ELEVATED_SURFACE = Color.decode("#202326"); MESSAGE_AREA = Color.decode("#101214");
            PRIMARY_ACCENT = Color.decode("#19B86A"); PRIMARY_ACCENT_HOVER = Color.decode("#149B59");
            PRIMARY_ACCENT_LIGHT = Color.decode("#123B2A"); SECONDARY_ACCENT = Color.decode("#42C784");
            OUTGOING_BUBBLE = Color.decode("#064E3B"); INCOMING_BUBBLE = Color.decode("#242729");
            OUTGOING_TEXT = Color.WHITE; INCOMING_TEXT = Color.decode("#E8EAE9");
            PRIMARY_TEXT = Color.decode("#F1F3F2"); SECONDARY_TEXT = Color.decode("#C0C6C2");
            MUTED_TEXT = Color.decode("#909894"); ACCENT_TEXT = Color.WHITE;
            DIVIDER = Color.decode("#303436"); CARD_BORDER = Color.decode("#383D3F");
            HOVER_OVERLAY = new Color(25, 184, 106, 26); SELECTION_OVERLAY = Color.decode("#123B2A");
            DATE_PILL_BG = Color.decode("#292D2F");
        } else {
            MAIN_BG = Color.decode("#F5F4F0"); SIDEBAR_BG = Color.WHITE;
            ELEVATED_SURFACE = Color.WHITE; MESSAGE_AREA = Color.decode("#FAF9F6");
            PRIMARY_ACCENT = Color.decode("#286454"); PRIMARY_ACCENT_HOVER = Color.decode("#1D4F42");
            PRIMARY_ACCENT_LIGHT = Color.decode("#EAF2ED"); SECONDARY_ACCENT = Color.decode("#427965");
            OUTGOING_BUBBLE = Color.decode("#EAF2ED"); INCOMING_BUBBLE = Color.decode("#EEEFEA");
            OUTGOING_TEXT = Color.decode("#242B28"); INCOMING_TEXT = Color.decode("#242B28");
            PRIMARY_TEXT = Color.decode("#242B28"); SECONDARY_TEXT = Color.decode("#59635D");
            MUTED_TEXT = Color.decode("#768078"); ACCENT_TEXT = Color.WHITE;
            DIVIDER = Color.decode("#E5E7E1"); CARD_BORDER = Color.decode("#DFE3DB");
            HOVER_OVERLAY = new Color(40, 100, 84, 14); SELECTION_OVERLAY = Color.decode("#EAF2ED");
            DATE_PILL_BG = Color.decode("#DFE3DB");
        }
    }
}
