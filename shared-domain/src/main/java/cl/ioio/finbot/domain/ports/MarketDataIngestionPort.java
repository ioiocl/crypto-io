package cl.ioio.finbot.domain.ports;

/**
 * Input port for market data ingestion
 * Hexagonal architecture - driver port
 */
public interface MarketDataIngestionPort {
    
    /**
     * Start ingesting market data for specified symbols
     * @param symbols the symbols to track
     */
    void startIngestion(String... symbols);
    
    /**
     * Stop ingestion
     */
    void stopIngestion();
    
    /**
     * Check if ingestion is active
     * @return true if active
     */
    boolean isActive();
}
