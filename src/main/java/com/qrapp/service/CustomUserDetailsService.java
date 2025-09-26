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
                .authorities(user.getRole() != null ? user.getRole() : "ROLE_USER")
                .build();
    }

    private boolean isAdminRole(String role) {
        if (role == null)
            return false;
        String r = role.trim().toUpperCase();
        if (r.startsWith("ROLE_"))
            r = r.substring(5);
        return "ADMIN".equals(r);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !isAdminRole(u.getRole()))
                .toList();
    }

    public boolean deleteUserById(Long id) {
        return userRepository.findById(id).map(user -> {
            if (isAdminRole(user.getRole()))
                return false;
            try {
                qrCodeService.deleteQrCodesByUser(user);
            } catch (Exception e) {
                System.err.println("Failed to delete QR codes for user id " + user.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
            // permanently remove user when admin deletes
            userRepository.deleteById(user.getId());
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
            if (isAdminRole(u.getRole())) {
                continue;
            }
            try {
                qrCodeService.deleteQrCodesByUser(u);
            } catch (Exception e) {
                System.err.println("Failed to delete QR codes for user id " + u.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
            userRepository.deleteById(u.getId());
            deleted++;
        }
        return deleted;
    }

    public boolean blockUser(Long id) {
        return userRepository.findById(id).map(user -> {
            if (isAdminRole(user.getRole()))
                return false;
            user.setStatus("blocked");
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public boolean unblockUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setStatus("active");
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}