package com.qrapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "suspicious_activity")
@Data
@NoArgsConstructor
public class SuspiciousActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public SuspiciousActivity(Long userId, String username, String action) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.timestamp = LocalDateTime.now();
    }
}
