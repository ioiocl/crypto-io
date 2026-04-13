package cl.ioio.finbot.aidecision.adapter;

import cl.ioio.finbot.domain.model.MarketContext;
import cl.ioio.finbot.domain.model.MarketDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RedisDecisionPublisher {

    private static final String DECISION_CHANNEL = "market-decisions";
    private static final String CONTEXT_CHANNEL = "market-context-stream";
    private static final String DECISION_KEY_PREFIX = "latest_decision:";
    private static final String CONTEXT_KEY_PREFIX = "latest_context:";

    private final PubSubCommands<String> pubSub;
    private final ValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisDecisionPublisher(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.pubSub = redisDataSource.pubsub(String.class);
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
    }

    public void publishDecision(MarketDecision decision) {
        try {
            String json = objectMapper.writeValueAsString(decision);
            pubSub.publish(DECISION_CHANNEL, json);
            valueCommands.set(DECISION_KEY_PREFIX + decision.getSymbol(), json);
            log.debug("Published market decision for {}", decision.getSymbol());
        } catch (Exception e) {
            log.error("Error publishing market decision for {}", decision.getSymbol(), e);
        }
    }

    public void publishContext(MarketContext context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            pubSub.publish(CONTEXT_CHANNEL, json);
            valueCommands.set(CONTEXT_KEY_PREFIX + context.getSymbol(), json);
            log.debug("Published market context for {}", context.getSymbol());
        } catch (Exception e) {
            log.error("Error publishing market context for {}", context.getSymbol(), e);
        }
    }
}
