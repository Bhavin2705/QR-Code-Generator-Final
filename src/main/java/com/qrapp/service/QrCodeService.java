
package com.qrapp.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.qrapp.entity.QrCode;
import com.qrapp.entity.User;
import com.qrapp.repository.QrCodeRepository;
import com.qrapp.storage.DocumentStorageService;
import com.qrapp.storage.ImageStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QrCodeService {
    // Save or update an existing QR code
    public QrCode saveQrCode(QrCode qrCode) {
        return qrCodeRepository.save(qrCode);
    }

    @Autowired
    private ImageStorageService imageStorageService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.scheme:http}")
    private String serverScheme;

    @Value("${server.hostname:}")
    private String serverHostname;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    public QrCode saveQrCode(String inputText) {
        throw new IllegalStateException(
                "QR codes must be associated with a user. Use saveQrCode(inputText, type, user) instead.");
    }

    public QrCode saveQrCode(String inputText, String type) {
        throw new IllegalStateException(
                "QR codes must be associated with a user. Use saveQrCode(inputText, type, user) instead.");
    }

    public QrCode saveQrCode(String inputText, String type, User user) {
        QrCode qrCode = new QrCode(inputText, type, user);
        return qrCodeRepository.save(qrCode);
    }

    public long countByType(String type) {
        return qrCodeRepository.countByType(type);
    }

    public long countByTypeAndUser(String type, User user) {
        return qrCodeRepository.countByUserAndType(user, type);
    }

    public long countByUserAndType(User user, String type) {
        return qrCodeRepository.countByUserAndType(user, type);
    }

    public List<QrCode> getAllQrCodes() {
        return qrCodeRepository.findAllByOrderByTimestampDesc();
    }

    public List<QrCode> getQrCodesByUser(User user) {
        return qrCodeRepository.findByUserOrderByTimestampDesc(user);
    }

    public void deleteQrCode(Long id) {
        QrCode qrCode = qrCodeRepository.findById(id).orElse(null);
        if (qrCode != null) {
            String inputText = qrCode.getInputText();
            String imagePrefix = "/api/qr/image/";
            String docPrefix = "/api/qr/doc/";
            if (inputText != null) {
                int idx = inputText.indexOf(imagePrefix);
                if (idx != -1) {
                    String filename = inputText.substring(idx + imagePrefix.length());
                    try {
                        java.nio.file.Path path = imageStorageService.load(filename);
                        java.nio.file.Files.deleteIfExists(path);
                    } catch (Exception e) {
                    }
                }
                idx = inputText.indexOf(docPrefix);
                if (idx != -1) {
                    String filename = inputText.substring(idx + docPrefix.length());
                    try {
                        java.nio.file.Path path = documentStorageService.load(filename);
                        java.nio.file.Files.deleteIfExists(path);
                    } catch (Exception e) {
                    }
                }
            }
            qrCodeRepository.deleteById(id);
        }
    }

    public String generateQrCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public Map<String, Object> generateQrCodeFromImageFile(MultipartFile file) throws IOException, WriterException {
        throw new IllegalStateException(
                "QR codes must be associated with a user. Use generateQrCodeFromImageFile(file, user) instead.");
    }

    public Map<String, Object> generateQrCodeFromImageFile(MultipartFile file, User user)
            throws IOException, WriterException {
        if (file == null || file.isEmpty())
            throw new IOException("No file uploaded");
        String filename = imageStorageService.storeImage(file);
        String host = getNetworkHost();
        String portSuffix = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        String imageUrl = serverScheme + "://" + host + portSuffix + "/api/qr/image/" + filename;
        QrCode saved = saveQrCode(imageUrl, "generated", user);
        String qrCodeImage = generateQrCodeImage(imageUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("text", imageUrl);
        response.put("image", "data:image/png;base64," + qrCodeImage);
        response.put("timestamp", saved.getTimestamp());
        response.put("type", saved.getType());
        return response;
    }

    public Map<String, Object> generateQrCodeForDocument(MultipartFile file) throws IOException, WriterException {
        throw new IllegalStateException(
                "QR codes must be associated with a user. Use generateQrCodeForDocument(file, user) instead.");
    }

    public Map<String, Object> generateQrCodeForDocument(MultipartFile file, User user)
            throws IOException, WriterException {
        if (file == null || file.isEmpty())
            throw new IOException("No file uploaded");
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new IOException("File too large");
        }
        String filename = documentStorageService.storeDocument(file);
        String host = getNetworkHost();
        String portSuffix = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        String docUrl = serverScheme + "://" + host + portSuffix + "/api/qr/doc/" + filename;
        QrCode saved = saveQrCode(docUrl, "generated", user);
        String qrCodeImage = generateQrCodeImage(docUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("text", docUrl);
        response.put("image", "data:image/png;base64," + qrCodeImage);
        response.put("timestamp", saved.getTimestamp());
        response.put("type", saved.getType());
        return response;
    }

    public String getNetworkHost() {
        if (!serverHostname.isEmpty()) {
            return serverHostname;
        }
        String fallback192 = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback())
                    continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("10."))
                            return ip;
                        if (ip.startsWith("172.")) {
                            String[] parts = ip.split("\\.");
                            int second = Integer.parseInt(parts[1]);
                            if (second >= 16 && second <= 31)
                                return ip;
                        }
                        if (ip.startsWith("192.168.")) {
                            if (fallback192 == null)
                                fallback192 = ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect network address", e);
        }
        if (fallback192 != null)
            return fallback192;
        throw new RuntimeException(
                "No suitable network address found. Please configure server.hostname for public access.");
    }

    public Map<String, Object> scanQrCodeFromImage(MultipartFile file) throws IOException {
        throw new IllegalStateException(
                "QR codes must be associated with a user. Use scanQrCodeFromImage(file, user) instead.");
    }

    public Map<String, Object> scanQrCodeFromImage(MultipartFile file, User user) throws IOException {
        if (file == null || file.isEmpty())
            throw new IOException("No file uploaded");
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if (bufferedImage == null)
            throw new IOException("Invalid image file");
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new MultiFormatReader().decode(bitmap);
            String text = result.getText();
            QrCode saved = saveQrCode(text, "scanned", user);
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("text", text);
            response.put("timestamp", saved.getTimestamp());
            response.put("type", saved.getType());
            return response;
        } catch (Exception e) {
            throw new IOException("QR code not readable");
        }
    }

    public QrCode getQrCodeById(Long id) {
        return qrCodeRepository.findById(id).orElse(null);
    }

    public int getServerPort() {
        return serverPort;
    }
}