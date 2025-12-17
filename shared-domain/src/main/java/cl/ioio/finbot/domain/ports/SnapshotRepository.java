package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketSnapshot;

import java.util.Optional;

/**
 * Output port for storing and retrieving market snapshots
 * Hexagonal architecture - driven port
 */
public interface SnapshotRepository {
    
    /**
     * Save a market snapshot
     * @param snapshot the snapshot to save
     */
    void save(MarketSnapshot snapshot);
    
    /**
     * Retrieve the latest snapshot for a symbol
     * @param symbol the symbol to retrieve
     * @return the latest snapshot if available
     */
    Optional<MarketSnapshot> findLatest(String symbol);
    
    /**
     * Delete a snapshot for a symbol
     * @param symbol the symbol to delete
     */
    void delete(String symbol);
}
