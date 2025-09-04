package com.qrapp.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {
    private final String uploadDir = "uploaded-docs";

    public DocumentStorageService() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    public String storeDocument(MultipartFile file) throws IOException {
        String ext = getFileExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, file.getBytes());
        return filename;
    }

    public Path load(String filename) {
        return Paths.get(uploadDir).resolve(filename);
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? "" : filename.substring(dot + 1);
    }
}
