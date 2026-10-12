package com.connectai.util;

public final class ErrorMessages {
    private ErrorMessages() {}
    public static String from(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage() == null ? "Something went wrong. Please try again." : cause.getMessage();
        if (message.contains("infinite recursion") || message.contains("42P17"))
            return "The Supabase membership policies need repair. Run database/fix-conversation-policies.sql in the Supabase SQL editor.";
        if (message.contains("row-level security") || message.contains("42501")) {
            String detail = message;
            int jsonStart = message.indexOf('{');
            if (jsonStart >= 0) {
                try { detail = JsonUtil.getMapper().readTree(message.substring(jsonStart)).path("message").asText(message); }
                catch (Exception ignored) {}
            }
            return "Supabase rejected the write: " + detail + ". Run database/repair-chat-access.sql and sign in again.";
        }
        if (message.contains("JWT expired") || message.contains("PGRST301"))
            return "Your session has expired. Sign in again to continue.";
        if (message.contains("23503"))
            return "Your account profile is missing from the database. Check the profile setup in Supabase.";
        return message;
    }
}
