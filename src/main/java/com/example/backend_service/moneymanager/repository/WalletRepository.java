package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByStudentId(Long studentId);
    Optional<Wallet> findByIdAndStudentId(String id, Long studentId);
    void deleteByStudentId(Long studentId);
}
