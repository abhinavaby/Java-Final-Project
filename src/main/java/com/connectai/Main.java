package com.connectai;

import com.connectai.config.AppConfig;
import com.connectai.config.ThemeColors;
import com.connectai.ui.auth.SplashScreen;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * ConnectAI Main Application Entry Point.
 */
public class Main {
    public static void main(String[] args) {
        AppConfig.load();
        ThemeColors.applyDarkMode(AppConfig.isDarkMode());
        try {
            if (AppConfig.isDarkMode()) com.formdev.flatlaf.FlatDarkLaf.setup();
            else com.formdev.flatlaf.FlatLightLaf.setup();
            UIManager.put("Component.accentColor", ThemeColors.PRIMARY_ACCENT);
            UIManager.put("Panel.background", ThemeColors.MAIN_BG);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 12);
            UIManager.put("ScrollBar.width", 10);
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme: " + e.getMessage());
        }

        // Launch Splash Screen on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.startInitialization();
        });
    }
}
