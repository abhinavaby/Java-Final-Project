package com.connectai.repository;

import com.connectai.config.AppConfig;
import com.connectai.util.HttpClientUtil;
import com.connectai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StorageRepository {

    public CompletableFuture<String> uploadFile(String storagePath, byte[] data, String contentType, String accessToken) {
        String path = "/storage/v1/object/chat-attachments/" + encodePath(storagePath);
        return HttpClientUtil.postBinaryAsync(path, data, contentType, accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Storage upload failed: " + response.body());
                    }
                    return storagePath;
                });
    }

    public CompletableFuture<String> getSignedUrl(String storagePath, int expiresInSeconds, String accessToken) {
        String path = "/storage/v1/object/sign/chat-attachments/" + encodePath(storagePath);
        Map<String, Object> body = Map.of("expiresIn", expiresInSeconds);

        return HttpClientUtil.postAsync(path, JsonUtil.toJson(body), accessToken)
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Failed to generate signed URL");
                    }
                    try {
                        JsonNode node = JsonUtil.getMapper().readTree(response.body());
                        String signedUrlPath = node.path("signedURL").asText(null);
                        if (signedUrlPath != null) {
                            return signedUrlPath.startsWith("http") ? signedUrlPath
                                    : AppConfig.getSupabaseUrl().replaceAll("/+$", "")
                                    + (signedUrlPath.startsWith("/storage/v1/") ? "" : "/storage/v1") + signedUrlPath;
                        }
                    } catch (Exception ignored) {}
                    return AppConfig.getSupabaseUrl() + "/storage/v1/object/authenticated/chat-attachments/" + encodePath(storagePath);
                });
    }
    static String encodePath(String path) {
        return java.util.Arrays.stream(path.split("/"))
                .map(part -> java.net.URLEncoder.encode(part, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(java.util.stream.Collectors.joining("/"));
    }
}
