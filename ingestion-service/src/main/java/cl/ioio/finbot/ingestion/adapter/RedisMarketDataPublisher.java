package cl.ioio.finbot.ingestion.adapter;

import cl.ioio.finbot.domain.model.MarketTick;
import cl.ioio.finbot.domain.ports.MarketDataPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis adapter for publishing market data
 * Driven adapter - implements MarketDataPublisher port
 */
@ApplicationScoped
@Slf4j
public class RedisMarketDataPublisher implements MarketDataPublisher {
    
    private static final String DEFAULT_CHANNEL = "market-stream";
    
    private final PubSubCommands<String> commands;
    private final ObjectMapper objectMapper;
    
    @Inject
    public RedisMarketDataPublisher(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.commands = redisDataSource.pubsub(String.class);
        this.objectMapper = objectMapper;
        log.info("Redis publisher initialized");
    }
    
    @Override
    public void publish(MarketTick tick) {
        publishToChannel(DEFAULT_CHANNEL, tick);
    }
    
    @Override
    public void publishToChannel(String channel, MarketTick tick) {
        try {
            String json = objectMapper.writeValueAsString(tick);
            commands.publish(channel, json);
            log.debug("Published tick for {} to channel {}", tick.getSymbol(), channel);
        } catch (Exception e) {
            log.error("Error publishing tick to Redis", e);
        }
    }
}
