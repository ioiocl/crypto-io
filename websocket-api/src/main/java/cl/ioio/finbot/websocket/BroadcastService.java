package cl.ioio.finbot.websocket;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.websocket.adapter.RedisSnapshotRepositoryImpl;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Service for broadcasting market snapshots to WebSocket clients
 * Periodically reads from Redis and pushes to connected clients
 */
@ApplicationScoped
@Slf4j
public class BroadcastService {
    
    private final RedisSnapshotRepositoryImpl snapshotRepository;
    private final MarketDataWebSocket webSocket;
    private final List<String> symbols;
    
    @Inject
    public BroadcastService(
            RedisSnapshotRepositoryImpl snapshotRepository,
            MarketDataWebSocket webSocket,
            @ConfigProperty(name = "broadcast.symbols", 
                    defaultValue = "AAPL,GOOGL,MSFT,TSLA,AMZN") String symbolsConfig) {
        
        this.snapshotRepository = snapshotRepository;
        this.webSocket = webSocket;
        this.symbols = Arrays.asList(symbolsConfig.split(","));
        
        log.info("Broadcast service initialized for symbols: {}", symbols);
    }
    
    /**
     * Broadcast snapshots periodically (every 1 second by default)
     */
    @Scheduled(every = "${broadcast.interval:1s}")
    void broadcastSnapshots() {
        for (String symbol : symbols) {
            try {
                // Only broadcast if there are active connections for this symbol
                if (MarketDataWebSocket.getConnectionCount(symbol) > 0) {
                    // Use reactive method (non-blocking)
                    snapshotRepository.findLatestReactive(symbol)
                        .subscribe().with(
                            snapshotOpt -> {
                                if (snapshotOpt.isPresent()) {
                                    webSocket.broadcastSnapshot(symbol, snapshotOpt.get());
                                } else {
                                    log.debug("No snapshot available for {}", symbol);
                                }
                            },
                            error -> log.error("Error retrieving snapshot for " + symbol, error)
                        );
                }
            } catch (Exception e) {
                log.error("Error broadcasting snapshot for " + symbol, e);
            }
        }
    }
}
