package cl.ioio.finbot.websocket.adapter;

import cl.ioio.finbot.domain.model.MarketContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RedisMarketContextRepositoryImpl {

    private static final String KEY_PREFIX = "latest_context:";

    private final ReactiveValueCommands<String, String> reactiveCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisMarketContextRepositoryImpl(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        this.reactiveCommands = reactiveRedisDataSource.value(String.class);
        this.objectMapper = objectMapper;
    }

    public Uni<Optional<MarketContext>> findLatestReactive(String symbol) {
        if (symbol == null) {
            return Uni.createFrom().item(Optional.empty());
        }

        String key = KEY_PREFIX + symbol;
        return reactiveCommands.get(key)
                .onItem().transform(json -> {
                    if (json == null) {
                        return Optional.<MarketContext>empty();
                    }

                    try {
                        return Optional.of(objectMapper.readValue(json, MarketContext.class));
                    } catch (Exception e) {
                        log.error("Error parsing market context for {}", symbol, e);
                        return Optional.<MarketContext>empty();
                    }
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error reading market context for {}", symbol, error);
                    return Optional.empty();
                });
    }
}
