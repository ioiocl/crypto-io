package cl.ioio.finbot.aidecision.adapter;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RedisMarketSnapshotReader {

    private static final String SNAPSHOT_KEY_PREFIX = "latest_snapshot:";

    private final ValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisMarketSnapshotReader(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
    }

    public Optional<MarketSnapshot> findLatest(String symbol) {
        try {
            String json = valueCommands.get(SNAPSHOT_KEY_PREFIX + symbol);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, MarketSnapshot.class));
        } catch (Exception e) {
            log.error("Error reading latest snapshot for {}", symbol, e);
            return Optional.empty();
        }
    }
}
