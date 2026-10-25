package com.connectai.repository;

import com.connectai.model.Profile;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProfileRepository {

    public CompletableFuture<Profile> getProfileById(String userId, String accessToken) {
        String path = "/rest/v1/profiles?id=eq." + userId + "&select=*";
        return HttpClientUtil.getAsync(path, accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to fetch profile: " + response.body());
                    }
                    List<Profile> list = JsonUtil.fromJsonList(response.body(), Profile.class);
                    return list.isEmpty() ? null : list.get(0);
                });
    }

    public CompletableFuture<List<Profile>> searchProfiles(String query, String currentUserId, String accessToken) {
        String pattern = URLEncoder.encode("*" + query.trim().replace("*", "") + "*", StandardCharsets.UTF_8);
        String base = "&id=neq." + URLEncoder.encode(currentUserId, StandardCharsets.UTF_8) + "&select=*&limit=20";
        CompletableFuture<List<Profile>> usernames = searchColumn("username", pattern, base, accessToken);
        CompletableFuture<List<Profile>> names = searchColumn("display_name", pattern, base, accessToken);
        return usernames.thenCombine(names, (byUsername, byName) -> {
            java.util.LinkedHashMap<String, Profile> merged = new java.util.LinkedHashMap<>();
            byUsername.forEach(p -> merged.put(p.getId(), p));
            byName.forEach(p -> merged.putIfAbsent(p.getId(), p));
            return merged.values().stream().limit(20).toList();
        });
    }

    private CompletableFuture<List<Profile>> searchColumn(String column, String pattern, String base, String accessToken) {
        return HttpClientUtil.getAsync("/rest/v1/profiles?" + column + "=ilike." + pattern + base, accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) throw new RuntimeException("Failed to search profiles: " + response.body());
                    return JsonUtil.fromJsonList(response.body(), Profile.class);
                });
    }

    public CompletableFuture<Profile> updateProfile(Profile profile, String accessToken) {
        String path = "/rest/v1/profiles?id=eq." + profile.getId();
        Map<String, Object> body = new HashMap<>();
        body.put("display_name", profile.getDisplayName());
        body.put("about", profile.getAbout());
        if (profile.getAvatarPath() != null) {
            body.put("avatar_path", profile.getAvatarPath());
        }

        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to update profile: " + response.body());
                    }
                    List<Profile> list = JsonUtil.fromJsonList(response.body(), Profile.class);
                    return list.isEmpty() ? profile : list.get(0);
                });
    }

    public CompletableFuture<Void> updateLastSeen(String userId, String accessToken) {
        String path = "/rest/v1/profiles?id=eq." + userId;
        Map<String, Object> body = new HashMap<>();
        body.put("last_seen", java.time.Instant.now().toString());
        return HttpClientUtil.patchAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(r -> null);
    }
}
