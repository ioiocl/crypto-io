package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketTick;
import io.smallrye.mutiny.Multi;

import java.util.function.Consumer;

/**
 * Input port for subscribing to market data
 * Hexagonal architecture - driver port
 */
public interface MarketDataSubscriber {
    
    /**
     * Subscribe to market data updates (blocking/callback style)
     * @param channel the channel to subscribe to
     * @param handler the handler for incoming ticks
     */
    void subscribe(String channel, Consumer<MarketTick> handler);
    
    /**
     * Subscribe to market data updates (reactive stream)
     * @param channel the channel to subscribe to
     * @return Multi stream of market ticks with backpressure support
     */
    Multi<MarketTick> subscribeReactive(String channel);
    
    /**
     * Unsubscribe from a channel
     * @param channel the channel to unsubscribe from
     */
    void unsubscribe(String channel);
}
