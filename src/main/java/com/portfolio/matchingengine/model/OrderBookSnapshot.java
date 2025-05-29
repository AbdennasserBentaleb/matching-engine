package com.portfolio.matchingengine.model;

import java.math.BigDecimal;
import java.util.Map;

public record OrderBookSnapshot(
        String ticker,
        Map<BigDecimal, BigDecimal> bids,
        Map<BigDecimal, BigDecimal> asks) {
}
