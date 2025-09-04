package com.qrapp.repository;

import java.util.List;

import com.qrapp.entity.QrCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    List<QrCode> findAllByOrderByTimestampDesc();

    long countByType(String type);
}
