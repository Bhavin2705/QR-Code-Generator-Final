package com.qrapp.controller;

import com.qrapp.entity.QrCode;
import com.qrapp.service.QrCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class QrTextWebController {

    @Autowired
    private QrCodeService qrCodeService;

    @GetMapping("/qr/{id}")
    public String showQrText(@PathVariable Long id, Model model) {
        try {
            QrCode qrCode = qrCodeService.getQrCodeById(id);

            if (qrCode != null) {
                String content = qrCode.getInputText();

                // Check if the content is a URL
                if (isUrl(content)) {
                    // Redirect to the URL
                    return "redirect:" + content;
                } else {
                    // Display the text content
                    model.addAttribute("content", content);
                    model.addAttribute("type", qrCode.getType());
                    model.addAttribute("timestamp", qrCode.getTimestamp());
                    model.addAttribute("text", content); // Keep for backward compatibility
                    return "qr-text";
                }
            } else {
                model.addAttribute("error", "QR Code not found");
                model.addAttribute("text", "QR code not found or invalid link.");
                return "qr-error";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading QR Code: " + e.getMessage());
            model.addAttribute("text", "Error loading QR code.");
            return "qr-error";
        }
    }

    private boolean isUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        text = text.trim().toLowerCase();
        return text.startsWith("http://") ||
                text.startsWith("https://") ||
                text.startsWith("ftp://") ||
                text.startsWith("www.");
    }
}
