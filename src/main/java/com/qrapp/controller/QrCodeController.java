package com.qrapp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qrapp.entity.QrCode;
import com.qrapp.entity.User;
import com.qrapp.service.AuthService;
import com.qrapp.service.QrCodeService;
import com.qrapp.storage.DocumentStorageService;
import com.qrapp.storage.ImageStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "*")
public class QrCodeController {

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private AuthService authService;

    @Value("${server.scheme:http}")
    private String serverScheme;

    @Value("${server.port:8080}")
    private int serverPort;

    // Helper method to extract user from HttpServletRequest
    private User getUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return authService.getUserFromToken(token);
        }
        return null;
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Resource file = new UrlResource(imageStorageService.load(filename).toUri());
            if (!file.exists() || !file.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(imageStorageService.load(filename));
            MediaType mediaType;
            try {
                mediaType = contentType != null ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM;
            } catch (Exception ex) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .contentType(mediaType)
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/doc/{filename}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String filename) {
        try {
            Resource file = new UrlResource(documentStorageService.load(filename).toUri());
            if (!file.exists() || !file.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(documentStorageService.load(filename));
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .contentType(mediaType)
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateQrCode(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            User user = getUserFromRequest(httpRequest);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }

            String inputText = request.get("text");
            String type = request.getOrDefault("type", "generated");
            QrCode savedQrCode = qrCodeService.saveQrCode(inputText, type, user);

            String host = qrCodeService.getNetworkHost();
            String portSuffix = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
            String textUrl = serverScheme + "://" + host + portSuffix + "/qr/" + savedQrCode.getId();
            String qrCodeImage = qrCodeService.generateQrCodeImage(textUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedQrCode.getId());
            response.put("text", inputText);
            response.put("url", textUrl);
            response.put("image", "data:image/png;base64," + qrCodeImage);
            response.put("timestamp", savedQrCode.getTimestamp());
            response.put("type", savedQrCode.getType());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/generate-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> generateQrFromImage(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            User user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }

            Map<String, Object> result = qrCodeService.generateQrCodeFromImageFile(file, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-doc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            User user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }

            Map<String, Object> result = qrCodeService.generateQrCodeForDocument(file, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Long> stats = new HashMap<>();
        stats.put("generated", qrCodeService.countByTypeAndUser("generated", user));
        stats.put("scanned", qrCodeService.countByTypeAndUser("scanned", user));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getQrCodeHistory(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<QrCode> codes = qrCodeService.getQrCodesByUser(user);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (QrCode code : codes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", code.getId());
            map.put("text", code.getInputText());
            map.put("type", code.getType());
            map.put("timestamp", code.getTimestamp());
            try {
                // Always use the original inputText for QR image, so it matches what was generated
                String qrCodeImage = qrCodeService.generateQrCodeImage(code.getInputText());
                map.put("image", "data:image/png;base64," + qrCodeImage);
            } catch (Exception e) {
                map.put("image", "");
                System.err.println("Failed to generate QR image for history id " + code.getId() + ": " + e.getMessage());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQrCode(@PathVariable Long id, HttpServletRequest request) {
        try {
            User user = getUserFromRequest(request);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }

            QrCode qrCode = qrCodeService.getQrCodeById(id);
            if (qrCode == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "QR Code not found"));
            }

            if (!qrCode.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            qrCodeService.deleteQrCode(id);
            return ResponseEntity.ok(Map.of("message", "QR Code deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<?> updateQrCodeText(@PathVariable Long id, @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        User user = getUserFromRequest(httpRequest);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        QrCode qrCode = qrCodeService.getQrCodeById(id);
        if (qrCode == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "QR Code not found"));
        }
        if (!qrCode.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to edit this QR code."));
        }
        String newText = request.get("text");
        if (newText == null || newText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "QR content cannot be empty"));
        }
        if (newText.length() > 750) {
            return ResponseEntity.badRequest().body(Map.of("error", "QR content exceeds 750 characters"));
        }
        qrCode.setInputText(newText);
        qrCodeService.saveQrCode(qrCode);
        return ResponseEntity.ok(Map.of("message", "QR updated successfully", "id", qrCode.getId(), "text", qrCode.getInputText()));
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> scanQrCode(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            System.out.println("Scan request received - File: " + (file != null ? file.getOriginalFilename() : "null") +
                    ", Size: " + (file != null ? file.getSize() : "null"));

            User user = getUserFromRequest(request);
            if (user == null) {
                System.out.println("Authentication failed for scan request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }

            System.out.println("User authenticated: " + user.getUsername());

            if (file == null || file.isEmpty()) {
                System.out.println("File is null or empty");
                return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
            }

            Map<String, Object> result = qrCodeService.scanQrCodeFromImage(file, user);
            System.out.println("Scan successful");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            System.out.println("IOException during scan: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println("Exception during scan: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}