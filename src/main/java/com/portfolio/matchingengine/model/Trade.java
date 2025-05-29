package com.portfolio.matchingengine.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Trade(
        String id,
        String buyOrderId,
        String sellOrderId,
        BigDecimal price,
        BigDecimal quantity,
        Instant timestamp) {
}
