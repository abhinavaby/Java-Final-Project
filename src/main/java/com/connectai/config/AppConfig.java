package com.connectai.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages application configuration properties loaded from application.properties or environment variables.
 */
public class AppConfig {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;

        // 1. Try loading from local application.properties file in working directory
        File localFile = new File("application.properties");
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                properties.load(is);
            } catch (Exception e) {
                System.err.println("Failed to load application.properties: " + e.getMessage());
            }
        }

        // 2. Fallback/merge classpath application.properties
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties classpathProps = new Properties();
                classpathProps.load(is);
                for (String name : classpathProps.stringPropertyNames()) {
                    if (!properties.containsKey(name)) {
                        properties.put(name, classpathProps.getProperty(name));
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Override with environment variables if present
        String envUrl = System.getenv("SUPABASE_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            properties.setProperty("supabase.url", envUrl);
        }

        String envKey = System.getenv("SUPABASE_ANON_KEY");
        if (envKey != null && !envKey.isBlank()) {
            properties.setProperty("supabase.anon.key", envKey);
        }

        String envOpenAi = System.getenv("OPENAI_API_KEY");
        if (envOpenAi != null && !envOpenAi.isBlank()) {
            properties.setProperty("openai.api.key", envOpenAi);
        }

        loaded = true;
    }

    private static Boolean mockModeOverride = null;

    public static boolean isMockMode() {
        load();
        if (mockModeOverride != null) return mockModeOverride;
        String prop = properties.getProperty("app.mock_mode", "");
        if ("false".equalsIgnoreCase(prop.trim())) return false;
        if ("true".equalsIgnoreCase(prop.trim())) return true;

        // Auto-enable mock mode only if Supabase is completely unconfigured
        return !isSupabaseConfigured();
    }

    public static boolean isSupabaseConfigured() {
        load();
        String url = getSupabaseUrl();
        String key = getSupabaseAnonKey();
        return !url.isBlank() && !url.contains("xyzcompany.supabase.co") && !key.isBlank() && !key.contains("example_anon_key");
    }

    public static synchronized void updateSupabaseCredentials(String url, String anonKey) {
        load();
        properties.setProperty("supabase.url", url.trim());
        properties.setProperty("supabase.anon.key", anonKey.trim());
        properties.setProperty("app.mock_mode", "false");
        mockModeOverride = false;
        saveProperties();
    }

    public static synchronized void saveProperties() {
        try {
            // Save to current working directory application.properties
            File localFile = new File("application.properties");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(localFile)) {
                properties.store(fos, "ConnectAI Application Configuration");
            }

            // Also update src/main/resources/application.properties if it exists in dev tree
            File resFile = new File("src/main/resources/application.properties");
            if (resFile.exists()) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(resFile)) {
                    properties.store(fos, "ConnectAI Application Configuration");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to save application.properties: " + e.getMessage());
        }
    }

    public static void setMockMode(boolean mockMode) {
        mockModeOverride = mockMode;
        properties.setProperty("app.mock_mode", String.valueOf(mockMode));
    }

    public static String getSupabaseUrl() {
        load();
        return properties.getProperty("supabase.url", "https://xyzcompany.supabase.co").trim();
    }

    public static String getSupabaseAnonKey() {
        load();
        return properties.getProperty("supabase.anon.key", "").trim();
    }

    public static String getOpenAiApiKey() {
        load();
        return properties.getProperty("openai.api.key", "").trim();
    }

    public static synchronized void updateOpenAiKey(String key) {
        load();
        properties.setProperty("openai.api.key", key.trim());
        saveProperties();
    }

    public static String getAppName() {
        load();
        return properties.getProperty("app.name", "ConnectAI");
    }

    public static String getAppVersion() {
        load();
        return properties.getProperty("app.version", "1.0.0");
    }
}
