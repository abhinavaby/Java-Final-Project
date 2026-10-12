package com.connectai.repository;

import com.connectai.model.User;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthRepository {

    public CompletableFuture<User> signUp(String email, String password, String username, String displayName, String avatarPath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("display_name", displayName);
        if (avatarPath != null) metadata.put("avatar_path", avatarPath);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("data", metadata);

        return HttpClientUtil.postAsync("/auth/v1/signup", JsonUtil.toJson(body), null)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException(extractErrorMessage(response.body(), "Registration failed"));
                    }
                    // If email confirmation is required, Supabase returns user data but no access_token.
                    // We return a partial User object; the app will prompt the user to confirm their email.
                    return parseUserFromSignupJson(response.body());
                });
    }

    public CompletableFuture<User> signIn(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        return HttpClientUtil.postAsync("/auth/v1/token?grant_type=password", JsonUtil.toJson(body), null)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException(extractErrorMessage(response.body(), "Invalid email or password"));
                    }
                    return parseUserFromAuthJson(response.body());
                });
    }

    public CompletableFuture<User> refreshSession(String refreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        return HttpClientUtil.postAsync("/auth/v1/token?grant_type=refresh_token", JsonUtil.toJson(body), null)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException(extractErrorMessage(response.body(), "Session expired"));
                    }
                    return parseUserFromAuthJson(response.body());
                });
    }

    public CompletableFuture<Boolean> sendPasswordReset(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);

        return HttpClientUtil.postAsync("/auth/v1/recover", JsonUtil.toJson(body), null)
                .thenApply(response -> response.statusCode() < 400);
    }

    private User parseUserFromAuthJson(String json) {
        try {
            JsonNode node = JsonUtil.getMapper().readTree(json);
            String accessToken = node.path("access_token").asText(null);
            String refreshToken = node.path("refresh_token").asText(null);
            long expiresIn = node.path("expires_in").asLong(3600);

            JsonNode userNode = node.has("user") ? node.path("user") : node;
            String id = userNode.path("id").asText(null);
            String email = userNode.path("email").asText(null);

            User user = new User(id, email, accessToken, refreshToken);
            user.setExpiresIn(expiresIn);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse auth response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the signup response from Supabase.
     * When email confirmation is enabled, the response contains only user data (no access_token).
     * When email confirmation is disabled, the response contains a full session with access_token.
     */
    private User parseUserFromSignupJson(String json) {
        try {
            JsonNode node = JsonUtil.getMapper().readTree(json);

            // Check if response has a session (email confirmation disabled)
            if (node.has("access_token") && !node.path("access_token").asText("").isBlank()) {
                return parseUserFromAuthJson(json);
            }

            // Email confirmation required — extract user fields
            JsonNode userNode = node.has("user") ? node.path("user") : node;
            String id = userNode.path("id").asText(null);
            String email = userNode.path("email").asText(null);

            if (id == null || id.isBlank()) {
                throw new RuntimeException("Signup failed: unable to parse user ID from response");
            }

            // Return partial user (no tokens yet — user must confirm email first)
            return new User(id, email, null, null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse signup response: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        try {
            JsonNode node = JsonUtil.getMapper().readTree(responseBody);
            if (node.has("error_description")) return node.get("error_description").asText();
            if (node.has("msg")) return node.get("msg").asText();
            if (node.has("message")) return node.get("message").asText();
        } catch (Exception ignored) {}
        return defaultMsg;
    }
}
