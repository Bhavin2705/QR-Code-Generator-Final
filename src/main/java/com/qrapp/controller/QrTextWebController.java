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
        QrCode qr = null;
        try {
            qr = qrCodeService.getQrCodeById(id);
        } catch (Exception e) {
            // Optionally log error
        }
        if (qr == null) {
            model.addAttribute("text", "QR code not found or invalid link.");
        } else {
            model.addAttribute("text", qr.getInputText());
        }
        return "qr-text";
    }
}
