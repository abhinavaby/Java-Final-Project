package com.connectai;

import com.connectai.config.AppConfig;
import com.connectai.model.Message;
import com.connectai.model.MessageType;
import com.connectai.model.User;
import com.connectai.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    public void testConfigLoader() {
        assertNotNull(AppConfig.getAppName());
        assertEquals("ConnectAI", AppConfig.getAppName());
    }

    @Test
    public void testUserJsonSerialization() {
        User user = new User("u-123", "test@connectai.com", "token-abc", "refresh-xyz");
        String json = JsonUtil.toJson(user);
        assertNotNull(json);

        User deserialized = JsonUtil.fromJson(json, User.class);
        assertEquals(user.getId(), deserialized.getId());
        assertEquals(user.getEmail(), deserialized.getEmail());
    }

    @Test
    public void testMessageModel() {
        Message msg = new Message();
        msg.setId("msg-1");
        msg.setMessageType(MessageType.TEXT);
        msg.setContent("Hello ConnectAI");

        assertEquals("msg-1", msg.getId());
        assertEquals(MessageType.TEXT, msg.getMessageType());
        assertFalse(msg.isDeleted());
    }
    @Test
    public void validatesProjectUrlAndPublicKey() {
        assertDoesNotThrow(() -> AppConfig.validateSupabaseCredentials("https://example.supabase.co/", "sb_publishable_test"));
        assertDoesNotThrow(() -> AppConfig.validateSupabaseCredentials("http://localhost:54321", "local-public-key"));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.validateSupabaseCredentials("https://example.supabase.co/rest/v1", "public-key"));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.validateSupabaseCredentials("https://example.supabase.co", "sb_secret_private"));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.validateSupabaseCredentials("not-a-url", "public-key"));
    }

    @Test
    public void rejectsServiceRoleKeyInDesktopClient() {
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"role\":\"service_role\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () ->
                AppConfig.validateSupabaseCredentials("https://example.supabase.co", "eyJhbGciOiJIUzI1NiJ9." + payload + ".signature"));
    }
    @Test
    public void groupErrorsExplainDatabaseRepair() {
        Throwable wrapped = new java.util.concurrent.CompletionException(
                new RuntimeException("Failed to create group: {\"code\":\"42P17\",\"message\":\"infinite recursion detected in policy\"}"));
        assertTrue(com.connectai.util.ErrorMessages.from(wrapped).contains("fix-conversation-policies.sql"));
        assertTrue(com.connectai.util.ErrorMessages.from(new RuntimeException("JWT expired")).contains("Sign in again"));
    }

    @Test
    public void rejectsAuthSuccessWithoutSession() throws Exception {
        var method = com.connectai.repository.AuthRepository.class.getDeclaredMethod("parseUserFromAuthJson", String.class);
        method.setAccessible(true);
        var repository = new com.connectai.repository.AuthRepository();
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(repository,
                "{\"user\":{\"id\":\"u-123\",\"email\":\"test@example.com\"}}"));
        User user = (User) method.invoke(repository,
                "{\"access_token\":\"token\",\"refresh_token\":\"refresh\",\"user\":{\"id\":\"u-123\",\"email\":\"test@example.com\"}}");
        assertEquals("token", user.getAccessToken());
    }
}
