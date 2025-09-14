package com.qrapp.repository;

import java.util.List;

import com.qrapp.entity.QrCode;
import com.qrapp.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    List<QrCode> findAllByOrderByTimestampDesc();

    List<QrCode> findByUserOrderByTimestampDesc(User user);

    long countByType(String type);

    long countByUserAndType(User user, String type);
}
