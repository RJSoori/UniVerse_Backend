package com.example.backend_service.marketplace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordUnitsSoldRequest(@NotNull @Min(1) Integer quantity) {
}
