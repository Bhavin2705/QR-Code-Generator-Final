package com.qrapp.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
public class ImageStorageServiceTest {
    @Autowired
    private ImageStorageService imageStorageService;

    @Test
    void testStoreImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[] { 1, 2, 3 });
        String filename = imageStorageService.storeImage(file);
        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
    }
}
