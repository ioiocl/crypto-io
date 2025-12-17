package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketTick;

/**
 * Output port for publishing market data
 * Hexagonal architecture - driven port
 */
public interface MarketDataPublisher {
    
    /**
     * Publish a market tick to the internal message bus
     * @param tick the market tick to publish
     */
    void publish(MarketTick tick);
    
    /**
     * Publish to a specific channel
     * @param channel the channel name
     * @param tick the market tick
     */
    void publishToChannel(String channel, MarketTick tick);
}
