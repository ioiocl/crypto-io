package cl.ioio.finbot.news.adapter;

import cl.ioio.finbot.domain.model.NewsSignal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RedisNewsSignalPublisher {

    private static final String NEWS_CHANNEL = "news-stream";
    private static final String NEWS_KEY_PREFIX = "latest_news_signal:";

    private final PubSubCommands<String> pubSub;
    private final ValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisNewsSignalPublisher(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.pubSub = redisDataSource.pubsub(String.class);
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
    }

    public void publish(NewsSignal signal) {
        try {
            String json = objectMapper.writeValueAsString(signal);
            pubSub.publish(NEWS_CHANNEL, json);
            valueCommands.set(NEWS_KEY_PREFIX + signal.getSymbol(), json);
            log.debug("Published news signal for {}", signal.getSymbol());
        } catch (Exception e) {
            log.error("Error publishing news signal for {}", signal.getSymbol(), e);
        }
    }
}
