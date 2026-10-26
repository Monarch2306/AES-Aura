package com.aes.answersheets;

import com.aes.common.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path temp;

    @Test void rejectsWrongMimeType() {
        var service = new FileStorageService(1024, "image/jpeg,image/png,image/webp", temp.toString());
        var file = new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes());
        assertThrows(BadRequestException.class, () -> service.store(file));
    }

    @Test void storesWithGeneratedSafeName() {
        var service = new FileStorageService(1024, "image/jpeg,image/png,image/webp", temp.toString());
        var file = new MockMultipartFile("file", "../../evil.png", "image/png", new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a});
        var stored = service.store(file);
        assertTrue(stored.storedName().endsWith(".png"));
        assertFalse(stored.storedName().contains(".."));
        assertEquals("evil.png", stored.originalDisplayName());
    }
}
