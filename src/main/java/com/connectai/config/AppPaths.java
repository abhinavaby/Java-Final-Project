package com.connectai.config;

import java.nio.file.Path;

/** Writable per-user storage for installed desktop applications. */
public final class AppPaths {
    private AppPaths() {}
    public static Path dataDirectory() {
        String override = System.getProperty("connectai.dataDir");
        if (override != null && !override.isBlank()) return Path.of(override);
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return Path.of(local == null ? home : local, "ConnectAI");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "ConnectAI");
        return Path.of(home, ".local", "share", "connectai");
    }
}
