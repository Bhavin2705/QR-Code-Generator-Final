package com.qrapp;

import java.util.Optional;

import com.qrapp.entity.User;
import com.qrapp.repository.UserRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class QrGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(QrGeneratorApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner createAdminUser(UserRepository userRepository,
            BCryptPasswordEncoder encoder) {
        return args -> {
            Optional<User> adminOpt = userRepository.findByUsernameAndStatusNot("admin", "deleted");
            if (adminOpt.isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin.secure@qrapp.com");
                String strongPassword = "Adm1n!2025_Secure";
                admin.setPassword(encoder.encode(strongPassword));
                admin.setRole("ROLE_ADMIN");
                admin.setStatus("active");
                userRepository.save(admin);
                System.out.println("Admin user created: username=admin, password=" + strongPassword);
            } else {
                System.out.println("Admin user already exists.");
            }
        };
    }

    @Bean
    public org.springframework.boot.CommandLineRunner cleanupQrCodesOnStartup(
            com.qrapp.service.QrCodeService qrCodeService) {
        return args -> {
            int deleted = qrCodeService.cleanupOrphanedQrCodes();
            System.out.println("Cleaned up " + deleted + " orphaned QR code records on startup.");
        };
    }
}
