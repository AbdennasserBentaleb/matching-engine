package com.portfolio.matchingengine.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(
        String id,
        String ticker,
        Side side,
        BigDecimal price,
        BigDecimal quantity,
        Instant timestamp) {
}
