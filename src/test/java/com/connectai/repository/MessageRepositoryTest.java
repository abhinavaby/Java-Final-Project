package com.connectai.repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageRepositoryTest {
    @Test void readsTextMessagesWithEmptyReverseRelationships() {
        var messages = MessageRepository.parseMessages("""
                [{"id":"m1","sender_id":"u1","content":"Hello","message_type":"TEXT",
                  "attachment":[],"poll":[],"sender_profile":{"id":"u1","display_name":"Alex"},"reactions":[]}]
                """);
        assertEquals("Hello", messages.get(0).getContent());
        assertNull(messages.get(0).getAttachment());
        assertNull(messages.get(0).getPoll());
        assertEquals("Alex", messages.get(0).getSenderProfile().getDisplayName());
    }
    @Test void readsEmbeddedAttachmentAndPollArrays() {
        var messages = MessageRepository.parseMessages("""
                [{"id":"m2","attachment":[{"id":"a1","storage_path":"files/report.pdf"}],
                 "poll":[{"id":"p1","question":"Ready?","options":[]}]}]
                """);
        assertEquals("a1", messages.get(0).getAttachment().getId());
        assertEquals("p1", messages.get(0).getPoll().getId());
    }
    @Test void acceptsNoMessages() {
        assertTrue(MessageRepository.parseMessages("[]").isEmpty());
    }
}
