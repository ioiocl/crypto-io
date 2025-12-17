package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.model.MarketTick;

/**
 * Domain service port for market analysis
 * Hexagonal architecture - application service
 */
public interface AnalysisService {
    
    /**
     * Process a new market tick and update analysis
     * @param tick the new market tick
     */
    void processTick(MarketTick tick);
    
    /**
     * Generate a complete market snapshot for a symbol
     * @param symbol the symbol to analyze
     * @return the market snapshot with all analytics
     */
    MarketSnapshot generateSnapshot(String symbol);
}
