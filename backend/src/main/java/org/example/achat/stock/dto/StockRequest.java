package org.example.achat.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(
		@NotBlank String libelleStock,
		@NotNull @PositiveOrZero Integer qte,
		@NotNull @PositiveOrZero Integer qteMin) {
}
