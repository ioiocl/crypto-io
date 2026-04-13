package cl.ioio.finbot.domain.ports;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

/**
 * Output port for storing and retrieving market snapshots
 * Hexagonal architecture - driven port
 */
public interface SnapshotRepository {
    
    /**
     * Save a market snapshot (blocking)
     * @param snapshot the snapshot to save
     */
    void save(MarketSnapshot snapshot);
    
    /**
     * Save a market snapshot (reactive)
     * @param snapshot the snapshot to save
     * @return Uni that completes when save is done
     */
    Uni<Void> saveReactive(MarketSnapshot snapshot);
    
    /**
     * Retrieve the latest snapshot for a symbol (blocking)
     * @param symbol the symbol to retrieve
     * @return the latest snapshot if available
     */
    Optional<MarketSnapshot> findLatest(String symbol);
    
    /**
     * Retrieve the latest snapshot for a symbol (reactive)
     * @param symbol the symbol to retrieve
     * @return Uni with the latest snapshot if available
     */
    Uni<Optional<MarketSnapshot>> findLatestReactive(String symbol);
    
    /**
     * Delete a snapshot for a symbol (blocking)
     * @param symbol the symbol to delete
     */
    void delete(String symbol);
    
    /**
     * Delete a snapshot for a symbol (reactive)
     * @param symbol the symbol to delete
     * @return Uni that completes when delete is done
     */
    Uni<Void> deleteReactive(String symbol);
}
