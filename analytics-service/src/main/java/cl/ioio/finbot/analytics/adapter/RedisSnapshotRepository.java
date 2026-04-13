package cl.ioio.finbot.analytics.adapter;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.ports.SnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Redis adapter for storing market snapshots
 * Driven adapter - implements SnapshotRepository port
 * Supports both blocking and reactive operations
 */
@ApplicationScoped
@Slf4j
public class RedisSnapshotRepository implements SnapshotRepository {
    
    private static final String KEY_PREFIX = "latest_snapshot:";
    
    private final ValueCommands<String, String> commands;
    private final ReactiveRedisDataSource reactiveRedis;
    private final ObjectMapper objectMapper;
    
    @Inject
    public RedisSnapshotRepository(
            RedisDataSource redisDataSource, 
            ReactiveRedisDataSource reactiveRedis,
            ObjectMapper objectMapper) {
        this.commands = redisDataSource.value(String.class, String.class);
        this.reactiveRedis = reactiveRedis;
        this.objectMapper = objectMapper;
        log.info("Redis snapshot repository initialized (blocking + reactive)");
    }
    
    @Override
    public void save(MarketSnapshot snapshot) {
        if (snapshot == null || snapshot.getSymbol() == null) {
            log.warn("Cannot save null snapshot or snapshot without symbol");
            return;
        }
        
        try {
            String key = KEY_PREFIX + snapshot.getSymbol();
            String json = objectMapper.writeValueAsString(snapshot);
            commands.set(key, json);
            log.debug("Saved snapshot for {}", snapshot.getSymbol());
        } catch (Exception e) {
            log.error("Error saving snapshot for " + snapshot.getSymbol(), e);
        }
    }
    
    @Override
    public Optional<MarketSnapshot> findLatest(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        
        try {
            String key = KEY_PREFIX + symbol;
            String json = commands.get(key);
            
            if (json != null) {
                MarketSnapshot snapshot = objectMapper.readValue(json, MarketSnapshot.class);
                return Optional.of(snapshot);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving snapshot for " + symbol, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public void delete(String symbol) {
        if (symbol == null) {
            return;
        }
        
        try {
            String key = KEY_PREFIX + symbol;
            commands.getdel(key);
            log.debug("Deleted snapshot for {}", symbol);
        } catch (Exception e) {
            log.error("Error deleting snapshot for " + symbol, e);
        }
    }
    
    @Override
    public Uni<Void> saveReactive(MarketSnapshot snapshot) {
        if (snapshot == null || snapshot.getSymbol() == null) {
            log.warn("Cannot save null snapshot or snapshot without symbol");
            return Uni.createFrom().voidItem();
        }
        
        return Uni.createFrom().item(snapshot)
            .onItem().transformToUni(snap -> {
                try {
                    String key = KEY_PREFIX + snap.getSymbol();
                    String json = objectMapper.writeValueAsString(snap);
                    
                    return reactiveRedis.value(String.class)
                        .set(key, json)
                        .onItem().invoke(() -> log.debug("Saved snapshot for {} (reactive)", snap.getSymbol()))
                        .replaceWithVoid();
                        
                } catch (Exception e) {
                    log.error("Error serializing snapshot for " + snap.getSymbol(), e);
                    return Uni.createFrom().voidItem();
                }
            })
            .onFailure().invoke(e -> log.error("Error saving snapshot (reactive)", e))
            .onFailure().recoverWithNull();
    }
    
    @Override
    public Uni<Optional<MarketSnapshot>> findLatestReactive(String symbol) {
        if (symbol == null) {
            return Uni.createFrom().item(Optional.empty());
        }
        
        String key = KEY_PREFIX + symbol;
        
        return reactiveRedis.value(String.class)
            .get(key)
            .onItem().transform(json -> {
                if (json != null) {
                    try {
                        MarketSnapshot snapshot = objectMapper.readValue(json, MarketSnapshot.class);
                        return Optional.of(snapshot);
                    } catch (Exception e) {
                        log.error("Error deserializing snapshot for " + symbol, e);
                        return Optional.<MarketSnapshot>empty();
                    }
                }
                return Optional.<MarketSnapshot>empty();
            })
            .onFailure().invoke(e -> log.error("Error retrieving snapshot for " + symbol + " (reactive)", e))
            .onFailure().recoverWithItem(Optional.empty());
    }
    
    @Override
    public Uni<Void> deleteReactive(String symbol) {
        if (symbol == null) {
            return Uni.createFrom().voidItem();
        }
        
        String key = KEY_PREFIX + symbol;
        
        return reactiveRedis.value(String.class)
            .getdel(key)
            .onItem().invoke(() -> log.debug("Deleted snapshot for {} (reactive)", symbol))
            .replaceWithVoid()
            .onFailure().invoke(e -> log.error("Error deleting snapshot for " + symbol + " (reactive)", e))
            .onFailure().recoverWithNull();
    }
}
