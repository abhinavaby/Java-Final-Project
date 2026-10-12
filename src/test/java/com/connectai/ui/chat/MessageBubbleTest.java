package com.connectai.ui.chat;
import com.connectai.model.Message;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MessageBubbleTest {
    static { System.setProperty("java.awt.headless", "true"); }
    @Test void shortMessagesStayCompact() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            Message message = new Message();
            message.setContent("hi");
            MessageBubble bubble = new MessageBubble(message, true, null, null, null);
            assertTrue(bubble.getPreferredSize().width < 200, "Short message should fit its content");
            assertEquals(bubble.getPreferredSize().height, bubble.getMaximumSize().height);
        });
    }
}
