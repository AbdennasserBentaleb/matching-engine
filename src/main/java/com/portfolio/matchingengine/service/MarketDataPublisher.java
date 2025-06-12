package com.portfolio.matchingengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.matchingengine.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MarketDataPublisher {

    private static final Logger log = LoggerFactory.getLogger(MarketDataPublisher.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketDataPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    @SuppressWarnings("null")
    public void handleTradeExecutedEvent(Trade trade) {
        try {
            String message = objectMapper.writeValueAsString(trade);
            redisTemplate.convertAndSend("trades_channel", message);
            log.info("Published trade {} to Redis", trade.id());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trade event", e);
        }
    }
}
