package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.model.Order;
import com.portfolio.matchingengine.model.OrderBookSnapshot;
import com.portfolio.matchingengine.model.Side;
import com.portfolio.matchingengine.model.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class OrderBook {
    private final String ticker;
    private final TreeMap<BigDecimal, Deque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<BigDecimal, Deque<Order>> asks = new TreeMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public OrderBook(String ticker) {
        this.ticker = ticker;
    }

    public List<Trade> processOrder(Order incomingOrder) {
        lock.lock();
        try {
            List<Trade> trades = new ArrayList<>();
            Order currentOrder = incomingOrder;

            if (currentOrder.side() == Side.BUY) {
                currentOrder = matchAgainst(currentOrder, asks, trades, Side.BUY);
                if (currentOrder.quantity().compareTo(BigDecimal.ZERO) > 0) {
                    bids.computeIfAbsent(currentOrder.price(), k -> new LinkedList<>()).addLast(currentOrder);
                }
            } else {
                currentOrder = matchAgainst(currentOrder, bids, trades, Side.SELL);
                if (currentOrder.quantity().compareTo(BigDecimal.ZERO) > 0) {
                    asks.computeIfAbsent(currentOrder.price(), k -> new LinkedList<>()).addLast(currentOrder);
                }
            }

            return trades;
        } finally {
            lock.unlock();
        }
    }

    private Order matchAgainst(Order incoming, TreeMap<BigDecimal, Deque<Order>> opposingBook, List<Trade> trades,
            Side incomingSide) {
        Iterator<Map.Entry<BigDecimal, Deque<Order>>> levelIterator = opposingBook.entrySet().iterator();

        while (levelIterator.hasNext() && incoming.quantity().compareTo(BigDecimal.ZERO) > 0) {
            Map.Entry<BigDecimal, Deque<Order>> level = levelIterator.next();
            BigDecimal levelPrice = level.getKey();

            // Check if prices cross
            if (incomingSide == Side.BUY && incoming.price().compareTo(levelPrice) < 0) {
                break; // Incoming buy price is lower than the best ask
            }
            if (incomingSide == Side.SELL && incoming.price().compareTo(levelPrice) > 0) {
                break; // Incoming sell price is higher than the best bid
            }

            Deque<Order> ordersAtLevel = level.getValue();
            while (!ordersAtLevel.isEmpty() && incoming.quantity().compareTo(BigDecimal.ZERO) > 0) {
                Order restingOrder = ordersAtLevel.pollFirst();

                BigDecimal matchedQuantity = incoming.quantity().min(restingOrder.quantity());
                BigDecimal executionPrice = restingOrder.price(); // Execution happens at resting order's price

                String buyOrderId = incomingSide == Side.BUY ? incoming.id() : restingOrder.id();
                String sellOrderId = incomingSide == Side.SELL ? incoming.id() : restingOrder.id();

                Trade trade = new Trade(
                        UUID.randomUUID().toString(),
                        buyOrderId,
                        sellOrderId,
                        executionPrice,
                        matchedQuantity,
                        Instant.now());
                trades.add(trade);

                // Update quantities
                incoming = new Order(
                        incoming.id(), incoming.ticker(), incoming.side(), incoming.price(),
                        incoming.quantity().subtract(matchedQuantity), incoming.timestamp());

                BigDecimal restingRemaining = restingOrder.quantity().subtract(matchedQuantity);
                if (restingRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    // Put it back at the front to maintain time priority
                    Order updatedResting = new Order(
                            restingOrder.id(), restingOrder.ticker(), restingOrder.side(), restingOrder.price(),
                            restingRemaining, restingOrder.timestamp());
                    ordersAtLevel.addFirst(updatedResting);
                }
            }

            if (ordersAtLevel.isEmpty()) {
                levelIterator.remove();
            }
        }
        return incoming;
    }

    public OrderBookSnapshot getSnapshot() {
        lock.lock();
        try {
            Map<BigDecimal, BigDecimal> snapshotBids = new LinkedHashMap<>();
            for (Map.Entry<BigDecimal, Deque<Order>> entry : bids.entrySet()) {
                BigDecimal sum = entry.getValue().stream()
                        .map(Order::quantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                snapshotBids.put(entry.getKey(), sum);
            }

            Map<BigDecimal, BigDecimal> snapshotAsks = new LinkedHashMap<>();
            for (Map.Entry<BigDecimal, Deque<Order>> entry : asks.entrySet()) {
                BigDecimal sum = entry.getValue().stream()
                        .map(Order::quantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                snapshotAsks.put(entry.getKey(), sum);
            }

            return new OrderBookSnapshot(ticker, snapshotBids, snapshotAsks);
        } finally {
            lock.unlock();
        }
    }

    public String getTicker() {
        return ticker;
    }
}
