package cl.ioio.finbot.websocket;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.websocket.adapter.RedisSnapshotRepositoryImpl;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive service for streaming market snapshots
 * Provides Multi<MarketSnapshot> streams with backpressure support
 */
@ApplicationScoped
@Slf4j
public class ReactiveSnapshotStreamService {
    
    private final RedisSnapshotRepositoryImpl snapshotRepository;
    
    @Inject
    public ReactiveSnapshotStreamService(RedisSnapshotRepositoryImpl snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
        log.info("Reactive snapshot stream service initialized");
    }
    
    /**
     * Create a reactive stream of snapshots for a symbol
     * Emits snapshots at regular intervals with backpressure support
     * 
     * @param symbol the symbol to stream
     * @param intervalSeconds interval between emissions in seconds
     * @return Multi stream of market snapshots
     */
    public Multi<MarketSnapshot> streamSnapshots(String symbol, long intervalSeconds) {
        log.info("Creating reactive snapshot stream for {} with interval {}s", symbol, intervalSeconds);
        
        return Multi.createFrom().ticks()
            .every(Duration.ofSeconds(intervalSeconds))
            .onItem().transformToUniAndConcatenate(tick -> 
                snapshotRepository.findLatestReactive(symbol)
            )
            .onItem().transform(optional -> optional.orElse(null))
            .select().where(snapshot -> snapshot != null)
            .onItem().invoke(snapshot -> 
                log.debug("Emitting snapshot for {}: price={}", symbol, snapshot.getCurrentPrice())
            )
            .onFailure().invoke(e -> 
                log.error("Error in snapshot stream for " + symbol, e)
            )
            .onFailure().retry()
                .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10))
                .atMost(5)
            .onOverflow().drop();
    }
    
    /**
     * Create a reactive stream with default 1 second interval
     */
    public Multi<MarketSnapshot> streamSnapshots(String symbol) {
        return streamSnapshots(symbol, 1);
    }
    
    /**
     * Create a reactive stream that only emits when snapshot changes
     * Uses deduplication to avoid sending duplicate snapshots
     * 
     * @param symbol the symbol to stream
     * @param intervalSeconds polling interval in seconds
     * @return Multi stream of changed snapshots only
     */
    public Multi<MarketSnapshot> streamSnapshotsOnChange(String symbol, long intervalSeconds) {
        log.info("Creating reactive snapshot stream (on-change) for {} with interval {}s", 
            symbol, intervalSeconds);

        AtomicReference<String> lastSnapshotKey = new AtomicReference<>();
        
        return streamSnapshots(symbol, intervalSeconds)
            .select().where(snapshot -> {
                String snapshotKey = snapshot.getTimestamp() + ":" + snapshot.getCurrentPrice();
                String previousKey = lastSnapshotKey.getAndSet(snapshotKey);
                return !snapshotKey.equals(previousKey);
            })
            .onItem().invoke(snapshot -> 
                log.debug("Emitting changed snapshot for {}: price={}", 
                    symbol, snapshot.getCurrentPrice())
            );
    }
    
    /**
     * Create a high-frequency stream with backpressure handling
     * Suitable for real-time dashboards
     * 
     * @param symbol the symbol to stream
     * @return Multi stream with aggressive backpressure strategy
     */
    public Multi<MarketSnapshot> streamSnapshotsHighFrequency(String symbol) {
        log.info("Creating high-frequency snapshot stream for {}", symbol);
        
        return Multi.createFrom().ticks()
            .every(Duration.ofMillis(500))
            .onItem().transformToUniAndConcatenate(tick -> 
                snapshotRepository.findLatestReactive(symbol)
            )
            .onItem().transform(optional -> optional.orElse(null))
            .select().where(snapshot -> snapshot != null)
            .onOverflow().drop()
            .onFailure().retry()
                .withBackOff(Duration.ofMillis(500), Duration.ofSeconds(5))
                .indefinitely();
    }
}
