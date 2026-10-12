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

        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("release-defaults.properties")) {
            if (is != null) {
                Properties defaults = new Properties();
                defaults.load(is);
                for (String name : defaults.stringPropertyNames())
                    if (!properties.containsKey(name)) properties.setProperty(name, defaults.getProperty(name));
            }
        } catch (Exception ignored) {}

        // Installed apps keep user overrides outside the application bundle.
        java.nio.file.Path userConfig = AppPaths.dataDirectory().resolve("application.properties");
        if (java.nio.file.Files.exists(userConfig)) {
            try (InputStream is = java.nio.file.Files.newInputStream(userConfig)) {
                properties.load(is);
            } catch (Exception e) { System.err.println("Could not read user configuration."); }
        }

        // 3. Override with environment variables if present
        String envUrl = System.getenv("SUPABASE_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            properties.setProperty("supabase.url", envUrl);
        }

        String envKey = System.getenv("SUPABASE_PUBLISHABLE_KEY");
        if (envKey == null || envKey.isBlank()) envKey = System.getenv("SUPABASE_ANON_KEY");
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
        return false;
    }

    public static boolean isSupabaseConfigured() {
        load();
        String url = getSupabaseUrl();
        String key = getSupabaseAnonKey();
        return !url.isBlank() && !url.contains("xyzcompany.supabase.co") && !key.isBlank() && !key.contains("example_anon_key");
    }

    public static void validateSupabaseCredentials(String url, String key) {
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            if (uri.getHost() == null || !("https".equals(uri.getScheme())
                    || ("http".equals(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))))
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Enter the project URL, for example https://your-project.supabase.co.");
        }
        if (key == null || key.isBlank() || key.contains("...") || key.startsWith("sb_secret_")) {
            throw new IllegalArgumentException("Use a publishable or anon key from your project settings.");
        }
        if (key.startsWith("ey")) {
            try {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(key.split("\\.")[1]), java.nio.charset.StandardCharsets.UTF_8);
                if (payload.contains("service_role")) throw new IllegalArgumentException("Use the public anon key, not the service role key.");
            } catch (IllegalArgumentException e) { throw e; }
            catch (Exception e) { throw new IllegalArgumentException("The anon key is incomplete. Copy the full key."); }
        }
    }

    public static synchronized void updateSupabaseCredentials(String url, String anonKey) {
        load();
        validateSupabaseCredentials(url, anonKey);
        properties.setProperty("supabase.url", url.trim().replaceAll("/+$", ""));
        properties.setProperty("supabase.anon.key", anonKey.trim());
        properties.setProperty("app.mock_mode", "false");
        mockModeOverride = false;
        saveProperties();
    }

    public static synchronized void saveProperties() {
        try {
            java.nio.file.Path directory = AppPaths.dataDirectory();
            java.nio.file.Files.createDirectories(directory);
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(directory.resolve("application.properties"))) {
                properties.store(out, "ConnectAI user settings");
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
