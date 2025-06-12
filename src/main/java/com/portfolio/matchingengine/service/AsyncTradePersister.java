package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.entity.TradeEntity;
import com.portfolio.matchingengine.model.Trade;
import com.portfolio.matchingengine.repository.TradeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTradePersister {

    private final TradeRepository tradeRepository;

    public AsyncTradePersister(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Async
    @EventListener
    public void handleTradeExecutedEvent(Trade trade) {
        TradeEntity entity = new TradeEntity(
                trade.id(),
                trade.buyOrderId(),
                trade.sellOrderId(),
                trade.price(),
                trade.quantity(),
                trade.timestamp());
        tradeRepository.save(entity);
    }
}
