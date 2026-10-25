package com.connectai.util;

import com.connectai.config.AppConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpClientUtil {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static CompletableFuture<Void> checkSupabaseConnection(String url, String key) {
        AppConfig.validateSupabaseCredentials(url, key);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim().replaceAll("/+$", "") + "/auth/v1/settings"))
                .timeout(Duration.ofSeconds(15)).header("apikey", key.trim()).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new IllegalStateException("The project rejected this key. Check that the URL and key belong to the same project.");
            if (response.statusCode() >= 300)
                throw new IllegalStateException("Could not connect to Supabase (HTTP " + response.statusCode() + "). Check the project URL and project status.");
            return null;
        });
    }

    public static CompletableFuture<HttpResponse<String>> getAsync(String pathOrUrl, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        return client.sendAsync(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<byte[]>> getBytesAsync(String pathOrUrl, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Accept", "application/octet-stream");
        if (bearerToken != null && !bearerToken.isBlank()) builder.header("Authorization", "Bearer " + bearerToken);
        return client.sendAsync(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    public static CompletableFuture<HttpResponse<String>> postAsync(String pathOrUrl, String jsonBody, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Prefer", (pathOrUrl.startsWith("/rest/v1/conversations?") || pathOrUrl.startsWith("/rest/v1/messages?") || pathOrUrl.startsWith("/rest/v1/attachments?") || pathOrUrl.contains("?select="))
                        ? "return=representation" : "return=minimal");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpRequest.BodyPublisher bodyPublisher = (jsonBody != null && !jsonBody.isBlank())
                ? HttpRequest.BodyPublishers.ofString(jsonBody)
                : HttpRequest.BodyPublishers.noBody();

        return client.sendAsync(builder.POST(bodyPublisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> postBinaryAsync(String pathOrUrl, byte[] data, String contentType, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Content-Type", contentType)
                .header("x-upsert", "false");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        return client.sendAsync(builder.POST(HttpRequest.BodyPublishers.ofByteArray(data)).build(), HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> patchAsync(String pathOrUrl, String jsonBody, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Prefer", "return=representation");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        return client.sendAsync(builder.method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody)).build(), HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> deleteAsync(String pathOrUrl, String bearerToken) {
        String fullUrl = buildFullUrl(pathOrUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", AppConfig.getSupabaseAnonKey())
                .header("Accept", "application/json")
                .header("Prefer", "return=representation");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        return client.sendAsync(builder.DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String buildFullUrl(String pathOrUrl) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        String baseUrl = AppConfig.getSupabaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String path = pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl;
        return baseUrl + path;
    }
}
