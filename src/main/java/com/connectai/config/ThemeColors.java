package com.connectai.config;

import java.awt.Color;

/**
 * Modern Clean SaaS Theme (Vibrant Blue & Light Slate Palette matching reference design).
 */
public class ThemeColors {
    // Backgrounds
    public static final Color MAIN_BG          = Color.decode("#F5F4F0"); // clean soft-slate window background
    public static final Color SIDEBAR_BG       = Color.decode("#FFFFFF"); // clean white sidebar
    public static final Color ELEVATED_SURFACE = Color.decode("#FFFFFF"); // card background
    public static final Color MESSAGE_AREA     = Color.decode("#FAF9F6"); // center chat canvas

    // Accents
    public static final Color PRIMARY_ACCENT       = Color.decode("#286454"); // vibrant royal blue (Brand Blue)
    public static final Color PRIMARY_ACCENT_HOVER = Color.decode("#1D4F42");
    public static final Color PRIMARY_ACCENT_LIGHT = Color.decode("#EAF2ED"); // light blue active tint
    public static final Color SECONDARY_ACCENT     = Color.decode("#427965"); // secondary blue

    // Message Bubbles
    public static final Color OUTGOING_BUBBLE  = Color.decode("#EAF2ED"); // solid vibrant blue
    public static final Color INCOMING_BUBBLE  = Color.decode("#EEEFEA"); // soft slate gray bubble
    public static final Color OUTGOING_TEXT    = Color.decode("#242B28"); // white text for outgoing
    public static final Color INCOMING_TEXT    = Color.decode("#242B28"); // dark text for incoming

    // Text
    public static final Color PRIMARY_TEXT   = Color.decode("#242B28"); // deep slate 900
    public static final Color SECONDARY_TEXT = Color.decode("#59635D"); // slate 600
    public static final Color MUTED_TEXT     = Color.decode("#768078"); // slate 400
    public static final Color ACCENT_TEXT    = Color.decode("#FFFFFF"); // white text on blue buttons

    // Status & Semantic
    public static final Color SUCCESS_ONLINE = Color.decode("#10B981"); // emerald green online dot
    public static final Color WARNING        = Color.decode("#F59E0B"); // amber gold
    public static final Color ERROR          = Color.decode("#EF4444"); // bright coral red
    public static final Color DIVIDER        = Color.decode("#E5E7E1"); // subtle crisp border/divider
    public static final Color CARD_BORDER    = Color.decode("#DFE3DB"); // soft component border

    // Interactive States & Pills
    public static final Color HOVER_OVERLAY     = new Color(40, 100, 84, 14);  // subtle blue hover
    public static final Color SELECTION_OVERLAY = Color.decode("#EAF2ED");      // light blue selection
    public static final Color DATE_PILL_BG      = Color.decode("#DFE3DB");      // "Today" date badge
    public static final Color BADGE_RED         = Color.decode("#EF4444");      // unread count badge
    public static final Color BADGE_BLUE        = Color.decode("#286454");      // blue pill badge
    public static final Color SPARKLE_GOLD      = Color.decode("#F59E0B");      // Catch Me Up sparkle

    // Avatar Color Palette (Pastels matching reference UI)
    public static final Color AVATAR_DARK_BG   = Color.decode("#334155"); // Alex / ConnectAI dark slate
    public static final Color AVATAR_DARK_FG   = Color.decode("#FFFFFF");
    public static final Color AVATAR_TEAL_BG   = Color.decode("#99F6E4"); // Tech & AI Team
    public static final Color AVATAR_TEAL_FG   = Color.decode("#0F766E");
    public static final Color AVATAR_ROSE_BG   = Color.decode("#FECDD3"); // Design Guild
    public static final Color AVATAR_ROSE_FG   = Color.decode("#BE123C");
    public static final Color AVATAR_PURPLE_BG = Color.decode("#DDD6FE"); // Marketing Team
    public static final Color AVATAR_PURPLE_FG = Color.decode("#6D28D9");
    public static final Color AVATAR_BLUE_BG   = Color.decode("#BFDBFE"); // Project R&D
    public static final Color AVATAR_BLUE_FG   = Color.decode("#1D4F42");
    public static final Color AVATAR_SALMON_BG = Color.decode("#FDA4AF"); // Sarah Jenkins
    public static final Color AVATAR_SALMON_FG = Color.decode("#9F1239");
    public static final Color AVATAR_AMBER_BG  = Color.decode("#FDE68A"); // David Chen
    public static final Color AVATAR_AMBER_FG  = Color.decode("#92400E");
    public static final Color AVATAR_MINT_BG   = Color.decode("#A7F3D0"); // Elena Rostova
    public static final Color AVATAR_MINT_FG   = Color.decode("#065F46");
    public static final Color AVATAR_GOLD_BG   = Color.decode("#FEF08A"); // Marcus Vance
    public static final Color AVATAR_GOLD_FG   = Color.decode("#854D0E");
}
