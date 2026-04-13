package cl.ioio.finbot.aidecision.adapter;

import cl.ioio.finbot.domain.model.NewsSignal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@ApplicationScoped
@Slf4j
public class RedisNewsSignalSubscriber {

    private final PubSubCommands<String> pubSubCommands;
    private final ObjectMapper objectMapper;

    private RedisSubscriber subscription;

    @Inject
    public RedisNewsSignalSubscriber(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.pubSubCommands = redisDataSource.pubsub(String.class);
        this.objectMapper = objectMapper;
    }

    public void subscribe(String channel, Consumer<NewsSignal> handler) {
        this.subscription = pubSubCommands.subscribe(channel, message -> {
            try {
                NewsSignal signal = objectMapper.readValue(message, NewsSignal.class);
                handler.accept(signal);
            } catch (Exception e) {
                log.error("Error deserializing news signal", e);
            }
        });
        log.info("Subscribed to {}", channel);
    }

    public void unsubscribe() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
