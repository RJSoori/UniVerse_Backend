package com.example.backend_service.moneymanager.repository;

import com.example.backend_service.moneymanager.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, String> {
}
