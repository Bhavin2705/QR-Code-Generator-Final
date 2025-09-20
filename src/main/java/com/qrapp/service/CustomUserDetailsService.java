package com.qrapp.service;

import java.util.List;

import com.qrapp.entity.User;
import com.qrapp.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private com.qrapp.service.SuspiciousActivityService suspiciousActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.qrapp.service.QrCodeService qrCodeService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndStatusNot(username, "deleted")
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole() != null && user.getRole().equalsIgnoreCase("admin") ? "ROLE_ADMIN" : "ROLE_USER")
                .build();
    }

    // Admin methods
    public List<User> getAllUsers() {
        // Hide admin accounts from the list
        return userRepository.findAll().stream()
                .filter(user -> !user.getRole().equalsIgnoreCase("admin"))
                .toList();
    }

    public boolean deleteUserById(Long id) {
        return userRepository.findById(id).map(user -> {
            if (user.getRole().equalsIgnoreCase("admin"))
                return false;
            // remove associated QR codes and files
            try {
                qrCodeService.deleteQrCodesByUser(user);
            } catch (Exception e) {
                // log and continue
            }
            userRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    public boolean markUserSuspicious(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("suspicious");
            userRepository.save(user);
            suspiciousActivityService.logActivity(user, "marked_suspicious");
            return true;
        }).orElse(false);
    }

    public boolean unmarkUserSuspicious(Long id) {
        return userRepository.findById(id).map(user -> {
            if ("suspicious".equals(user.getStatus())) {
                user.setStatus("active");
                userRepository.save(user);
                suspiciousActivityService.logActivity(user, "unmarked_suspicious");
                return true;
            }
            return false;
        }).orElse(false);
    }

    public int deleteAllUsers() {
        List<User> all = userRepository.findAll();
        int deleted = 0;
        for (User u : all) {
            if (u.getRole() != null && u.getRole().equalsIgnoreCase("admin")) {
                // skip admins
                continue;
            }
            try {
                qrCodeService.deleteQrCodesByUser(u);
            } catch (Exception e) {
            }
            userRepository.deleteById(u.getId());
            deleted++;
        }
        return deleted;
    }
}