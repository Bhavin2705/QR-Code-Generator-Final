
package com.qrapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;

import com.qrapp.entity.QrCode;
import com.qrapp.entity.User;
import com.qrapp.repository.QrCodeRepository;
import com.qrapp.storage.DocumentStorageService;
import com.qrapp.storage.ImageStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

public class QrCodeServiceTest {

    private QrCodeService qrCodeService;
    private ImageStorageService imageStorageService;
    private DocumentStorageService documentStorageService;
    private QrCodeRepository qrCodeRepository;
    private User testUser;

    @BeforeEach
    void setup() throws Exception {
        // Create a test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        qrCodeService = new QrCodeService();
        imageStorageService = new ImageStorageService() {
            @Override
            public String storeImage(MultipartFile file) throws IOException {
                // deterministic filename for tests
                return "file.png";
            }

            @Override
            public java.nio.file.Path load(String filename) {
                return java.nio.file.Paths.get("uploaded-images").resolve(filename);
            }
        };

        documentStorageService = new DocumentStorageService() {
            @Override
            public String storeDocument(MultipartFile file) throws IOException {
                return "doc.pdf";
            }

            @Override
            public java.nio.file.Path load(String filename) {
                return java.nio.file.Paths.get("uploaded-docs").resolve(filename);
            }
        };
        qrCodeRepository = mock(QrCodeRepository.class);

        // Inject mocks via reflection
        Field f1 = QrCodeService.class.getDeclaredField("imageStorageService");
        f1.setAccessible(true);
        f1.set(qrCodeService, imageStorageService);

        Field f2 = QrCodeService.class.getDeclaredField("documentStorageService");
        f2.setAccessible(true);
        f2.set(qrCodeService, documentStorageService);

        Field f3 = QrCodeService.class.getDeclaredField("qrCodeRepository");
        f3.setAccessible(true);
        f3.set(qrCodeService, qrCodeRepository);

        // Set server properties to stable values
        Field portF = QrCodeService.class.getDeclaredField("serverPort");
        portF.setAccessible(true);
        portF.setInt(qrCodeService, 8080);

        Field schemeF = QrCodeService.class.getDeclaredField("serverScheme");
        schemeF.setAccessible(true);
        schemeF.set(qrCodeService, "http");

        Field hostF = QrCodeService.class.getDeclaredField("serverHostname");
        hostF.setAccessible(true);
        hostF.set(qrCodeService, "127.0.0.1");
    }

    @Test
    void saveQrCode_delegatesToRepository() {
        when(qrCodeRepository.save(any(QrCode.class))).thenAnswer(invocation -> {
            QrCode q = invocation.getArgument(0);
            q.setId(42L);
            return q;
        });

        QrCode out = qrCodeService.saveQrCode("hello world", "generated", testUser);
        assertNotNull(out);
        assertEquals(42L, out.getId());

        ArgumentCaptor<QrCode> cap = ArgumentCaptor.forClass(QrCode.class);
        verify(qrCodeRepository).save(cap.capture());
        assertEquals("hello world", cap.getValue().getInputText());
        assertEquals("generated", cap.getValue().getType());
        assertEquals(testUser, cap.getValue().getUser());
    }

    @Test
    void generateQrCodeImage_returnsPngBase64() throws Exception {
        String base64 = qrCodeService.generateQrCodeImage("test-content");
        assertNotNull(base64);
        byte[] decoded = java.util.Base64.getDecoder().decode(base64);
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngSig = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        assertTrue(decoded.length > pngSig.length);
        for (int i = 0; i < pngSig.length; i++) {
            assertEquals(pngSig[i], decoded[i]);
        }
    }

    @Test
    void generateQrCodeFromImageFile_happyPath() throws Exception {
        // Make repository save return an object with id
        when(qrCodeRepository.save(any(QrCode.class))).thenAnswer(invocation -> {
            QrCode q = invocation.getArgument(0);
            q.setId(5L);
            return q;
        });

        MultipartFile mf = new TestMultipartFile("image/png", "file.png", new byte[] { 1, 2, 3, 4 });
        Map<String, Object> res = qrCodeService.generateQrCodeFromImageFile(mf, testUser);
        assertNotNull(res);
        assertEquals(5L, ((Number) res.get("id")).longValue());
        String text = (String) res.get("text");
        assertTrue(text.contains("/api/qr/image/file.png"));
        String image = (String) res.get("image");
        assertTrue(image.startsWith("data:image/png;base64,"));
        assertEquals("generated", res.get("type"));
    }

    // Minimal MultipartFile implementation for tests
    static class TestMultipartFile implements MultipartFile {
        private final String contentType;
        private final String originalFilename;
        private final byte[] content;

        TestMultipartFile(String contentType, String originalFilename, byte[] content) {
            this.contentType = contentType;
            this.originalFilename = originalFilename;
            this.content = content;
        }

        @Override
        public String getName() {
            return originalFilename != null ? originalFilename : "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content != null ? content : new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content != null ? content : new byte[0]);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            if (dest == null)
                throw new IllegalArgumentException("Destination file cannot be null");
            byte[] toWrite = content != null ? content : new byte[0];
            java.nio.file.Files.write(dest.toPath(), toWrite);
        }
    }
}
