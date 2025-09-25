package com.qrapp.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentStorageServiceTest {
    @Autowired
    private DocumentStorageService documentStorageService;

    @Test
    void testStoreDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        String filename = documentStorageService.storeDocument(file);
        assertNotNull(filename);
        assertTrue(filename.endsWith(".txt"));
    }
}
