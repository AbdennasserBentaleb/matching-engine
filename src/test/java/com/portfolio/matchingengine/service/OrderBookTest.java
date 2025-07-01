package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.model.Order;
import com.portfolio.matchingengine.model.Side;
import com.portfolio.matchingengine.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("AAPL");
    }

    @Test
    void testExactMatch() {
        Order sellOrder = new Order("sell1", "AAPL", Side.SELL, new BigDecimal("150.00"), new BigDecimal("10"),
                Instant.now());
        List<Trade> sellTrades = orderBook.processOrder(sellOrder);
        assertEquals(0, sellTrades.size());

        Order buyOrder = new Order("buy1", "AAPL", Side.BUY, new BigDecimal("150.00"), new BigDecimal("10"),
                Instant.now());
        List<Trade> buyTrades = orderBook.processOrder(buyOrder);

        assertEquals(1, buyTrades.size());
        Trade trade = buyTrades.get(0);
        assertEquals(0, trade.price().compareTo(new BigDecimal("150.00")));
        assertEquals(0, trade.quantity().compareTo(new BigDecimal("10")));
        assertEquals("buy1", trade.buyOrderId());
        assertEquals("sell1", trade.sellOrderId());
    }

    @Test
    void testPartialFill() {
        Order sell1 = new Order("sell1", "AAPL", Side.SELL, new BigDecimal("150.00"), new BigDecimal("10"),
                Instant.now());
        orderBook.processOrder(sell1);

        Order buy1 = new Order("buy1", "AAPL", Side.BUY, new BigDecimal("150.00"), new BigDecimal("4"), Instant.now());
        List<Trade> trades1 = orderBook.processOrder(buy1);
        assertEquals(1, trades1.size());
        assertEquals(0, trades1.get(0).quantity().compareTo(new BigDecimal("4")));

        Order buy2 = new Order("buy2", "AAPL", Side.BUY, new BigDecimal("150.00"), new BigDecimal("6"), Instant.now());
        List<Trade> trades2 = orderBook.processOrder(buy2);
        assertEquals(1, trades2.size());
        assertEquals(0, trades2.get(0).quantity().compareTo(new BigDecimal("6")));
        assertEquals("buy2", trades2.get(0).buyOrderId());
    }

    @Test
    void testPriceTimePriority() throws InterruptedException {
        Order sell1 = new Order("sell1", "AAPL", Side.SELL, new BigDecimal("150.00"), new BigDecimal("10"),
                Instant.now());
        Thread.sleep(1);
        Order sell2 = new Order("sell2", "AAPL", Side.SELL, new BigDecimal("149.00"), new BigDecimal("10"),
                Instant.now());
        Thread.sleep(1);
        Order sell3 = new Order("sell3", "AAPL", Side.SELL, new BigDecimal("150.00"), new BigDecimal("10"),
                Instant.now());

        orderBook.processOrder(sell1);
        orderBook.processOrder(sell2);
        orderBook.processOrder(sell3);

        Order buy1 = new Order("buy1", "AAPL", Side.BUY, new BigDecimal("150.00"), new BigDecimal("15"), Instant.now());
        List<Trade> trades = orderBook.processOrder(buy1);

        // First trade should execute against sell2 (best price: 149.00)
        assertEquals(2, trades.size());
        assertEquals("sell2", trades.get(0).sellOrderId());
        assertEquals(0, trades.get(0).price().compareTo(new BigDecimal("149.00")));
        assertEquals(0, trades.get(0).quantity().compareTo(new BigDecimal("10")));

        // Second trade should execute against sell1 (arrived before sell3 at 150.00)
        assertEquals("sell1", trades.get(1).sellOrderId());
        assertEquals(0, trades.get(1).price().compareTo(new BigDecimal("150.00")));
        assertEquals(0, trades.get(1).quantity().compareTo(new BigDecimal("5")));
    }
}
