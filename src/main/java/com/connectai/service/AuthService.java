package com.connectai.service;

import com.connectai.model.Profile;
import com.connectai.model.User;
import com.connectai.repository.AuthRepository;
import com.connectai.repository.ProfileRepository;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final AuthService instance = new AuthService();
    private static final File SESSION_FILE = com.connectai.config.AppPaths.dataDirectory().resolve("local-session.json").toFile();

    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;

    private User currentUser;
    private Profile currentProfile;

    private AuthService() {
        this.authRepository = new AuthRepository();
        this.profileRepository = new ProfileRepository();
    }

    public static AuthService getInstance() {
        return instance;
    }

    public CompletableFuture<User> signUp(String email, String password, String username, String displayName, String avatarPath) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            this.currentUser = com.connectai.mock.MockDataProvider.getInstance().getCurrentUser();
            this.currentProfile = com.connectai.mock.MockDataProvider.getInstance().getCurrentProfile();
            return CompletableFuture.completedFuture(this.currentUser);
        }
        return authRepository.signUp(email, password, username, displayName, avatarPath)
                .thenCompose(user -> {
                    this.currentUser = user;
                    // Only save session & load profile if signup gave us an access token
                    // (i.e. email confirmation is disabled in Supabase settings)
                    if (user.getAccessToken() != null && !user.getAccessToken().isBlank()) {
                        saveSession(user);
                        return loadProfileAfterAuthentication().thenApply(p -> user);
                    }
                    // Email confirmation required — return user without loading profile yet
                    this.currentUser = null; // clear until confirmed
                    return CompletableFuture.completedFuture(user);
                });
    }

    public CompletableFuture<User> signIn(String email, String password) {
        if (com.connectai.config.AppConfig.isMockMode()) {
            this.currentUser = com.connectai.mock.MockDataProvider.getInstance().getCurrentUser();
            this.currentProfile = com.connectai.mock.MockDataProvider.getInstance().getCurrentProfile();
            return CompletableFuture.completedFuture(this.currentUser);
        }
        return authRepository.signIn(email, password)
                .thenCompose(user -> {
                    this.currentUser = user;
                    saveSession(user);
                    return loadProfileAfterAuthentication().thenApply(p -> user);
                });
    }

    public CompletableFuture<User> restoreSession() {
        if (com.connectai.config.AppConfig.isMockMode()) {
            this.currentUser = com.connectai.mock.MockDataProvider.getInstance().getCurrentUser();
            this.currentProfile = com.connectai.mock.MockDataProvider.getInstance().getCurrentProfile();
            return CompletableFuture.completedFuture(this.currentUser);
        }

        if (!SESSION_FILE.exists()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No saved session"));
        }

        try {
            String json = Files.readString(SESSION_FILE.toPath());
            User savedUser = JsonUtil.fromJson(json, User.class);
            if (savedUser == null || savedUser.getRefreshToken() == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Invalid session format"));
            }

            return authRepository.refreshSession(savedUser.getRefreshToken())
                    .thenCompose(newUser -> {
                        this.currentUser = newUser;
                        saveSession(newUser);
                        return loadProfileAfterAuthentication().thenApply(p -> newUser);
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Profile> loadProfileAfterAuthentication() {
        currentProfile = null;
        profileLoadWarning = null;
        return loadCurrentProfile().exceptionally(error -> {
            // Authentication succeeded. A missing schema or RLS problem must not invalidate it.
            String name = currentUser.getEmail() == null ? "User" : currentUser.getEmail().split("@")[0];
            currentProfile = new Profile(currentUser.getId(), name.toLowerCase(), name, null);
            profileLoadWarning = "Signed in, but your profile could not be loaded. Check the Supabase database setup and profile policies.";
            return currentProfile;
        });
    }

    private String profileLoadWarning;

    public String consumeProfileLoadWarning() {
        String warning = profileLoadWarning;
        profileLoadWarning = null;
        return warning;
    }

    public CompletableFuture<Profile> loadCurrentProfile() {
        if (com.connectai.config.AppConfig.isMockMode()) {
            this.currentProfile = com.connectai.mock.MockDataProvider.getInstance().getCurrentProfile();
            return CompletableFuture.completedFuture(this.currentProfile);
        }
        if (currentUser == null) return CompletableFuture.completedFuture(null);
        return profileRepository.getProfileById(currentUser.getId(), currentUser.getAccessToken())
                .thenCompose(profile -> {
                    if (profile != null) {
                        this.currentProfile = profile;
                        return CompletableFuture.completedFuture(profile);
                    }
                    // Profile not found in DB: create fallback profile from user email
                    String fallbackName = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
                    Profile p = new Profile(currentUser.getId(), "user_" + currentUser.getId().replace("-", ""), fallbackName, null);
                    this.currentProfile = p;

                    // Upsert profile into Supabase
                    Map<String, Object> map = Map.of(
                            "id", currentUser.getId(),
                            "username", p.getUsername(),
                            "display_name", p.getDisplayName(),
                            "about", "Hey there! I am using ConnectAI."
                    );
                    return HttpClientUtil.postAsync("/rest/v1/profiles", JsonUtil.toJson(map), currentUser.getAccessToken())
                            .thenApply(response -> {
                                if (response.statusCode() >= 400) throw new IllegalStateException("Profile creation failed");
                                return p;
                            });
                });
    }

    public void logout() {
        this.currentUser = null;
        this.currentProfile = null;
        if (SESSION_FILE.exists()) {
            SESSION_FILE.delete();
        }
    }

    public User getCurrentUser() {
        if (com.connectai.config.AppConfig.isMockMode() && currentUser == null) {
            this.currentUser = com.connectai.mock.MockDataProvider.getInstance().getCurrentUser();
        }
        return currentUser;
    }

    public Profile getCurrentProfile() {
        if (com.connectai.config.AppConfig.isMockMode() && currentProfile == null) {
            this.currentProfile = com.connectai.mock.MockDataProvider.getInstance().getCurrentProfile();
            return currentProfile;
        }
        if (currentProfile != null) {
            return currentProfile;
        }
        if (currentUser != null) {
            String name = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
            currentProfile = new Profile(currentUser.getId(), name.toLowerCase(), name, null);
            return currentProfile;
        }
        return null;
    }

    public String getAccessToken() {
        if (com.connectai.config.AppConfig.isMockMode()) {
            return "mock-access-token";
        }
        return currentUser != null ? currentUser.getAccessToken() : null;
    }

    private void saveSession(User user) {
        try {
            String json = JsonUtil.toJson(user);
            Files.createDirectories(SESSION_FILE.toPath().getParent());
            if (!SESSION_FILE.exists()) {
                try {
                    Files.createFile(SESSION_FILE.toPath(), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                            java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                } catch (UnsupportedOperationException ignored) {
                    // Non-POSIX platforms use the operating system's per-user application data directory ACL.
                }
            }
            Files.writeString(SESSION_FILE.toPath(), json);
            try {
                Files.setPosixFilePermissions(SESSION_FILE.toPath(),
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows file permissions are inherited from the user-specific application data directory.
            }
        } catch (Exception e) {
            System.err.println("Failed to save session locally: " + e.getMessage());
        }
    }
}
