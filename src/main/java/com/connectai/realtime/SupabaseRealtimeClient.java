package com.connectai.realtime;

import com.connectai.config.AppConfig;
import com.connectai.model.Message;
import com.connectai.service.AuthService;
import com.connectai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SupabaseRealtimeClient {
    private static final SupabaseRealtimeClient instance = new SupabaseRealtimeClient();

    private WebSocketClient webSocketClient;
    private final List<RealtimeEventListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private boolean isConnected = false;
    private int refCounter = 1;
    private int reconnectAttempts = 0;
    private ScheduledFuture<?> heartbeatTask;

    private SupabaseRealtimeClient() {}

    public static SupabaseRealtimeClient getInstance() {
        return instance;
    }

    public synchronized void connect() {
        if (com.connectai.config.AppConfig.isMockMode()) {
            isConnected = true;
            notifyConnectionState(true);
            return;
        }
        if (isConnected || (webSocketClient != null && webSocketClient.isOpen())) return;
        if (!AppConfig.isSupabaseConfigured() || AuthService.getInstance().getAccessToken() == null) {
            notifyConnectionState(false);
            return;
        }

        String wsUrl = buildWebSocketUrl();
        if (wsUrl == null) return;

        try {
            webSocketClient = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    reconnectAttempts = 0;
                    notifyConnectionState(true);
                    startHeartbeat();
                    subscribeToRealtimeChannels();
                }

                @Override
                public void onMessage(String messageStr) {
                    handleIncomingPayload(messageStr);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    notifyConnectionState(false);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Realtime WebSocket Error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            scheduleReconnect();
        }
    }

    public synchronized void disconnect() {
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception ignored) {}
        }
        isConnected = false;
        notifyConnectionState(false);
    }

    public void addListener(RealtimeEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(RealtimeEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void sendTypingBroadcast(String conversationId, boolean isTyping) {
        if (!isConnected || webSocketClient == null) return;

        String userId = AuthService.getInstance().getCurrentUser() != null ? AuthService.getInstance().getCurrentUser().getId() : "";
        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", "realtime:room:" + conversationId);
        payload.put("event", "typing");
        payload.put("payload", Map.of("user_id", userId, "is_typing", isTyping));
        payload.put("ref", String.valueOf(refCounter++));

        webSocketClient.send(JsonUtil.toJson(payload));
    }

    private void startHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isDone()) return;
        heartbeatTask = executor.scheduleAtFixedRate(() -> {
            if (isConnected && webSocketClient != null && webSocketClient.isOpen()) {
                Map<String, Object> heartbeat = new HashMap<>();
                heartbeat.put("topic", "phoenix");
                heartbeat.put("event", "heartbeat");
                heartbeat.put("payload", Map.of());
                heartbeat.put("ref", String.valueOf(refCounter++));
                webSocketClient.send(JsonUtil.toJson(heartbeat));
            }
        }, 25, 25, TimeUnit.SECONDS);
    }

    private void subscribeToRealtimeChannels() {
        if (!isConnected || webSocketClient == null) return;

        Map<String, Object> joinPayload = new HashMap<>();
        joinPayload.put("topic", "realtime:public:messages");
        joinPayload.put("event", "phx_join");
        joinPayload.put("join_ref", String.valueOf(refCounter));
        joinPayload.put("payload", Map.of(
                "config", Map.of(
                        "broadcast", Map.of("ack", false, "self", false),
                        "presence", Map.of("enabled", false, "key", ""),
                        "postgres_changes", List.of(
                                Map.of("event", "*", "schema", "public", "table", "messages"),
                                Map.of("event", "*", "schema", "public", "table", "message_reactions"),
                                Map.of("event", "*", "schema", "public", "table", "poll_votes")
                        ),
                        "private", false
                ),
                "access_token", AuthService.getInstance().getAccessToken()
        ));
        joinPayload.put("ref", String.valueOf(refCounter++));

        webSocketClient.send(JsonUtil.toJson(joinPayload));
    }

    private void handleIncomingPayload(String messageStr) {
        try {
            JsonNode root = JsonUtil.getMapper().readTree(messageStr);
            String event = root.path("event").asText("");
            JsonNode payload = root.path("payload");

            if ("postgres_changes".equals(event)) {
                JsonNode data = payload.path("data");
                String table = data.path("table").asText("");
                String type = data.path("type").asText("");
                JsonNode record = data.path("record");

                if ("messages".equals(table)) {
                    if ("INSERT".equals(type)) {
                        Message msg = JsonUtil.getMapper().treeToValue(record, Message.class);
                        notifyNewMessage(msg);
                    } else if ("UPDATE".equals(type)) {
                        Message msg = JsonUtil.getMapper().treeToValue(record, Message.class);
                        notifyMessageEdited(msg);
                    }
                } else if ("poll_votes".equals(table) && "INSERT".equals(type)) {
                    com.connectai.model.PollVote vote = JsonUtil.getMapper().treeToValue(record, com.connectai.model.PollVote.class);
                    notifyPollVoted(vote);
                } else if ("message_reactions".equals(table)) {
                    JsonNode reactionRecord = "DELETE".equals(type) ? data.path("old_record") : record;
                    if (!reactionRecord.isMissingNode() && !reactionRecord.isNull()) {
                        com.connectai.model.MessageReaction reaction = JsonUtil.getMapper()
                                .treeToValue(reactionRecord, com.connectai.model.MessageReaction.class);
                        notifyReactionUpdated(reaction);
                    }
                }
            } else if ("phx_reply".equals(event)) {
                JsonNode reply = payload.path("response");
                if ("error".equals(payload.path("status").asText()) || reply.has("postgres_changes") && reply.path("postgres_changes").isEmpty()) {
                    System.err.println("Supabase realtime subscription was rejected: " + reply);
                }
            } else if ("typing".equals(event)) {
                String convId = root.path("topic").asText("").replace("realtime:room:", "");
                String userId = payload.path("user_id").asText("");
                boolean isTyping = payload.path("is_typing").asBoolean(false);
                notifyTyping(convId, userId, isTyping);
            }
        } catch (Exception ignored) {}
    }

    private void scheduleReconnect() {
        reconnectAttempts++;
        long delaySeconds = Math.min(60, (long) Math.pow(2, Math.min(reconnectAttempts, 6)));
        executor.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private String buildWebSocketUrl() {
        String base = AppConfig.getSupabaseUrl();
        if (base.isBlank()) return null;
        String wsBase = base.replace("http://", "ws://").replace("https://", "wss://");
        return wsBase + "/realtime/v1/websocket?apikey=" + AppConfig.getSupabaseAnonKey() + "&vsn=1.0.0";
    }

    private void notifyConnectionState(boolean connected) {
        synchronized (listeners) {
            for (RealtimeEventListener l : listeners) l.onConnectionStateChanged(connected);
        }
    }

    private void notifyNewMessage(Message msg) {
        synchronized (listeners) {
            for (RealtimeEventListener l : listeners) l.onNewMessage(msg);
        }
    }

    private void notifyMessageEdited(Message msg) {
        synchronized (listeners) {
            for (RealtimeEventListener l : listeners) l.onMessageEdited(msg);
        }
    }

    private void notifyTyping(String convId, String userId, boolean isTyping) {
        synchronized (listeners) {
            for (RealtimeEventListener l : listeners) l.onTypingStatusChanged(convId, userId, isTyping);
        }
    }

    private void notifyPollVoted(com.connectai.model.PollVote vote) {
        synchronized (listeners) {
            for (RealtimeEventListener listener : listeners) listener.onPollVoted(vote);
        }
    }

    private void notifyReactionUpdated(com.connectai.model.MessageReaction reaction) {
        synchronized (listeners) {
            for (RealtimeEventListener listener : listeners) listener.onReactionUpdated(reaction);
        }
    }
}
