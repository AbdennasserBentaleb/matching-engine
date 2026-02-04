package com.portfolio.matchingengine.dto;

import com.portfolio.matchingengine.model.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderRequestDTO(
        @NotBlank(message = "Ticker cannot be blank") String ticker,

        @NotNull(message = "Side cannot be null") Side side,

        @NotNull(message = "Price cannot be null") @DecimalMin(value = "0.01", message = "Price must be strictly positive") BigDecimal price,

        @NotNull(message = "Quantity cannot be null") @DecimalMin(value = "0.01", message = "Quantity must be strictly positive") BigDecimal quantity) {
}
