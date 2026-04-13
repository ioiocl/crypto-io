package cl.ioio.finbot.analytics.adapter;

import cl.ioio.finbot.domain.model.MarketTick;
import cl.ioio.finbot.domain.ports.MarketDataSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Redis adapter for subscribing to market data
 * Driver adapter - implements MarketDataSubscriber port
 */
@ApplicationScoped
@Slf4j
public class RedisMarketDataSubscriber implements MarketDataSubscriber {
    
    private final PubSubCommands<String> pubSubCommands;
    private final ReactiveRedisDataSource reactiveRedis;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Consumer<MarketTick>> handlers = new ConcurrentHashMap<>();
    private final Map<String, RedisSubscriber> subscriptions = new ConcurrentHashMap<>();
    
    @Inject
    public RedisMarketDataSubscriber(
            RedisDataSource redisDataSource, 
            ReactiveRedisDataSource reactiveRedis,
            ObjectMapper objectMapper) {
        this.pubSubCommands = redisDataSource.pubsub(String.class);
        this.reactiveRedis = reactiveRedis;
        this.objectMapper = objectMapper;
        log.info("Redis subscriber initialized (blocking + reactive)");
    }
    
    @Override
    public void subscribe(String channel, Consumer<MarketTick> handler) {
        handlers.put(channel, handler);
        
        // Subscribe using Quarkus Redis API
        RedisSubscriber subscription = pubSubCommands.subscribe(channel, message -> handleMessage(channel, message));
        
        subscriptions.put(channel, subscription);
        log.info("Subscribed to channel: {}", channel);
    }
    
    @Override
    public Multi<MarketTick> subscribeReactive(String channel) {
        log.info("Creating reactive subscription to channel: {}", channel);
        
        return reactiveRedis.pubsub(String.class)
            .subscribe(channel)
            .onItem().transform(message -> {
                try {
                    MarketTick tick = objectMapper.readValue(message, MarketTick.class);
                    log.debug("Received tick (reactive) from {}: {}", channel, tick.getSymbol());
                    return tick;
                } catch (Exception e) {
                    log.error("Error deserializing message from channel {}: {}", channel, message, e);
                    return null;
                }
            })
            .select().where(tick -> tick != null)
            .onFailure().invoke(e -> log.error("Error in reactive subscription to " + channel, e))
            .onFailure().retry().withBackOff(java.time.Duration.ofSeconds(1)).atMost(5)
            .onOverflow().buffer(1000);
    }
    
    @Override
    public void unsubscribe(String channel) {
        handlers.remove(channel);
        
        // Unsubscribe from Redis
        RedisSubscriber subscription = subscriptions.remove(channel);
        if (subscription != null) {
            subscription.unsubscribe();
        }
        
        log.info("Unsubscribed from channel: {}", channel);
    }
    
    private void handleMessage(String channel, String message) {
        try {
            MarketTick tick = objectMapper.readValue(message, MarketTick.class);
            
            Consumer<MarketTick> handler = handlers.get(channel);
            if (handler != null) {
                handler.accept(tick);
            } else {
                log.warn("No handler registered for channel: {}", channel);
            }
            
        } catch (Exception e) {
            log.error("Error handling message from channel {}: {}", channel, message, e);
        }
    }
}
