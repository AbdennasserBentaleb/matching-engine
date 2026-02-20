package com.portfolio.matchingengine.service;

import com.portfolio.matchingengine.model.Trade;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@Service
public class TradeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public TradeEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @SuppressWarnings("null")
    public void publishTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            applicationEventPublisher.publishEvent(trade);
        }
    }
}
