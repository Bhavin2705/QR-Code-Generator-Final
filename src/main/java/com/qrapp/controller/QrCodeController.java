package com.qrapp.controller;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qrapp.entity.QrCode;
import com.qrapp.service.QrCodeService;
import com.qrapp.storage.DocumentStorageService;
import com.qrapp.storage.ImageStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
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

    @Value("${server.scheme:http}")
    private String serverScheme;

    @Value("${server.port:8080}")
    private int serverPort;

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
    public ResponseEntity<Map<String, Object>> generateQrCode(@RequestBody Map<String, String> request) {
        try {
            String inputText = request.get("text");
            String type = request.getOrDefault("type", "generated");
            QrCode savedQrCode = qrCodeService.saveQrCode(inputText, type);

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
    public ResponseEntity<Map<String, Object>> generateQrFromImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = qrCodeService.generateQrCodeFromImageFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-doc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = qrCodeService.generateQrCodeForDocument(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("generated", qrCodeService.countByType("generated"));
        stats.put("scanned", qrCodeService.countByType("scanned"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getQrCodeHistory() {
        List<QrCode> codes = qrCodeService.getAllQrCodes();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (QrCode code : codes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", code.getId());
            map.put("text", code.getInputText());
            map.put("type", code.getType());
            map.put("timestamp", code.getTimestamp());
            try {
                String qrCodeImage = qrCodeService.generateQrCodeImage(code.getInputText());
                map.put("image", "data:image/png;base64," + qrCodeImage);
            } catch (Exception e) {
                map.put("image", "");
                System.err
                        .println("Failed to generate QR image for history id " + code.getId() + ": " + e.getMessage());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQrCode(@PathVariable Long id) {
        qrCodeService.deleteQrCode(id);
        return ResponseEntity.ok(Map.of("message", "QR Code deleted successfully"));
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> scanQrCode(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = qrCodeService.scanQrCodeFromImage(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}