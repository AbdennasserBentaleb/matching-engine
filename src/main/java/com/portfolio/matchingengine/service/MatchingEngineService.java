package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.model.Order;
import com.portfolio.matchingengine.model.OrderBookSnapshot;
import com.portfolio.matchingengine.model.Trade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchingEngineService {

    private final ConcurrentHashMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public List<Trade> processOrder(Order order) {
        OrderBook orderBook = orderBooks.computeIfAbsent(order.ticker(), OrderBook::new);
        return orderBook.processOrder(order);
    }

    public OrderBookSnapshot getOrderBookSnapshot(String ticker) {
        OrderBook orderBook = orderBooks.get(ticker);
        if (orderBook == null) {
            return new OrderBookSnapshot(ticker, Map.of(), Map.of());
        }
        return orderBook.getSnapshot();
    }
}
