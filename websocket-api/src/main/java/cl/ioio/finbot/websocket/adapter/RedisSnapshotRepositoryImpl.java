package cl.ioio.finbot.websocket.adapter;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.ports.SnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Redis adapter for reading market snapshots
 * Driven adapter - implements SnapshotRepository port
 */
@ApplicationScoped
@Slf4j
public class RedisSnapshotRepositoryImpl implements SnapshotRepository {
    
    private static final String KEY_PREFIX = "latest_snapshot:";
    
    private final ReactiveValueCommands<String, String> reactiveCommands;
    private final ObjectMapper objectMapper;
    
    @Inject
    public RedisSnapshotRepositoryImpl(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        this.reactiveCommands = reactiveRedisDataSource.value(String.class);
        this.objectMapper = objectMapper;
        log.info("Redis snapshot repository initialized (reactive)");
    }
    
    /**
     * Get snapshot reactively (non-blocking)
     */
    public Uni<Optional<MarketSnapshot>> findLatestReactive(String symbol) {
        if (symbol == null) {
            return Uni.createFrom().item(Optional.empty());
        }
        
        String key = KEY_PREFIX + symbol;
        return reactiveCommands.get(key)
            .onItem().transform(json -> {
                if (json != null) {
                    try {
                        MarketSnapshot snapshot = objectMapper.readValue(json, MarketSnapshot.class);
                        return Optional.of(snapshot);
                    } catch (Exception e) {
                        log.error("Error parsing snapshot for " + symbol, e);
                        return Optional.<MarketSnapshot>empty();
                    }
                }
                return Optional.<MarketSnapshot>empty();
            })
            .onFailure().recoverWithItem(error -> {
                log.error("Error retrieving snapshot for " + symbol, error);
                return Optional.empty();
            });
    }
    
    @Override
    public void save(MarketSnapshot snapshot) {
        // Not implemented in WebSocket API (read-only)
        throw new UnsupportedOperationException("WebSocket API is read-only");
    }
    
    @Override
    public Optional<MarketSnapshot> findLatest(String symbol) {
        // This method should not be called from WebSocket context
        // Use findLatestReactive instead to avoid blocking
        log.warn("Blocking findLatest called - use findLatestReactive instead");
        throw new UnsupportedOperationException("Use findLatestReactive for non-blocking access");
    }
    
    @Override
    public void delete(String symbol) {
        // Not implemented in WebSocket API (read-only)
        throw new UnsupportedOperationException("WebSocket API is read-only");
    }
}
