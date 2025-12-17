package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketTick;

import java.util.function.Consumer;

/**
 * Input port for subscribing to market data
 * Hexagonal architecture - driver port
 */
public interface MarketDataSubscriber {
    
    /**
     * Subscribe to market data updates
     * @param channel the channel to subscribe to
     * @param handler the handler for incoming ticks
     */
    void subscribe(String channel, Consumer<MarketTick> handler);
    
    /**
     * Unsubscribe from a channel
     * @param channel the channel to unsubscribe from
     */
    void unsubscribe(String channel);
}
