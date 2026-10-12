package com.connectai.repository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StorageRepositoryTest {
    @Test void encodesFilenameWithoutLosingFolderSeparators() {
        assertEquals("group/message_my%20photo%20%231.png", StorageRepository.encodePath("group/message_my photo #1.png"));
        assertEquals("group/report.pdf", StorageRepository.encodePath("group/report.pdf"));
    }
}
