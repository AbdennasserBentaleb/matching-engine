package com.portfolio.matchingengine.controller;

import com.portfolio.matchingengine.dto.OrderRequestDTO;
import com.portfolio.matchingengine.model.Order;
import com.portfolio.matchingengine.model.OrderBookSnapshot;
import com.portfolio.matchingengine.model.Trade;
import com.portfolio.matchingengine.service.MatchingEngineService;
import com.portfolio.matchingengine.service.TradeEventPublisher;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final MatchingEngineService matchingEngineService;
    private final TradeEventPublisher tradeEventPublisher;

    public OrderController(MatchingEngineService matchingEngineService, TradeEventPublisher tradeEventPublisher) {
        this.matchingEngineService = matchingEngineService;
        this.tradeEventPublisher = tradeEventPublisher;
    }

    @PostMapping("/orders")
    public ResponseEntity<List<Trade>> submitOrder(@Valid @RequestBody OrderRequestDTO request) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.ticker(),
                request.side(),
                request.price(),
                request.quantity(),
                Instant.now());

        List<Trade> trades = matchingEngineService.processOrder(order);

        if (!trades.isEmpty()) {
            tradeEventPublisher.publishTrades(trades);
        }

        return ResponseEntity.ok(trades);
    }

    @GetMapping("/orderbook/{ticker}")
    public ResponseEntity<OrderBookSnapshot> getOrderBook(@PathVariable String ticker) {
        return ResponseEntity.ok(matchingEngineService.getOrderBookSnapshot(ticker));
    }
}
