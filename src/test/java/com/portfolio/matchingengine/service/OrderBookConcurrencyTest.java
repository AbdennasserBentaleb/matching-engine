package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.model.Order;
import com.portfolio.matchingengine.model.Side;
import com.portfolio.matchingengine.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderBookConcurrencyTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("BTC-USD");
    }

    @Test
    void testConcurrentOrderMatching() throws InterruptedException, ExecutionException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<List<Trade>>> callables = new ArrayList<>();
        
        AtomicInteger orderIdCounter = new AtomicInteger();

        // 50 threads will post BUY orders, 50 threads will post SELL orders at the exact same price
        for (int i = 0; i < numberOfThreads; i++) {
            final int id = orderIdCounter.incrementAndGet();
            if (i % 2 == 0) {
                callables.add(() -> {
                    Order buy = new Order("buy" + id, "BTC-USD", Side.BUY, new BigDecimal("50000.00"), new BigDecimal("1"), Instant.now());
                    return orderBook.processOrder(buy);
                });
            } else {
                callables.add(() -> {
                    Order sell = new Order("sell" + id, "BTC-USD", Side.SELL, new BigDecimal("50000.00"), new BigDecimal("1"), Instant.now());
                    return orderBook.processOrder(sell);
                });
            }
        }

        List<Future<List<Trade>>> futures = executorService.invokeAll(callables);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        int totalTrades = 0;
        for (Future<List<Trade>> future : futures) {
            totalTrades += future.get().size();
        }

        // Each matched trade appears once in the return List of the side that crossed the spread.
        // With 50 buys and 50 sells of size 1 at the same price, we should have exactly 50 trades executed.
        assertEquals(50, totalTrades, "Concurrent limit orders at the same price should perfectly fill down to 0 without race conditions.");
        
        // Assert the orderbook is empty now
        assertEquals(BigDecimal.ZERO, orderBook.getSnapshot().bids().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(BigDecimal.ZERO, orderBook.getSnapshot().asks().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
