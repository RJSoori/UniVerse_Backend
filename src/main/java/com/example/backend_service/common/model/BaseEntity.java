package com.example.backend_service.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}