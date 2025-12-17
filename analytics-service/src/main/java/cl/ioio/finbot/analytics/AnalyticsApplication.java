package cl.ioio.finbot.analytics;

import cl.ioio.finbot.analytics.application.MarketAnalysisService;
import cl.ioio.finbot.analytics.adapter.RedisMarketDataSubscriber;
import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.model.MarketTick;
import cl.ioio.finbot.domain.ports.SnapshotRepository;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Main application for analytics service
 * Subscribes to market data and generates snapshots periodically
 */
@ApplicationScoped
@Slf4j
public class AnalyticsApplication {
    
    private static final String MARKET_STREAM_CHANNEL = "market-stream";
    
    private final RedisMarketDataSubscriber subscriber;
    private final MarketAnalysisService analysisService;
    private final SnapshotRepository snapshotRepository;
    private final List<String> symbols;
    
    @Inject
    public AnalyticsApplication(
            RedisMarketDataSubscriber subscriber,
            MarketAnalysisService analysisService,
            SnapshotRepository snapshotRepository,
            @ConfigProperty(name = "analytics.symbols", 
                    defaultValue = "AAPL,GOOGL,MSFT,TSLA,AMZN") String symbolsConfig) {
        
        this.subscriber = subscriber;
        this.analysisService = analysisService;
        this.snapshotRepository = snapshotRepository;
        this.symbols = Arrays.asList(symbolsConfig.split(","));
        
        log.info("Analytics application initialized for symbols: {}", symbols);
    }
    
    void onStart(@Observes StartupEvent event) {
        log.info("Starting analytics service");
        
        // Subscribe to market data stream
        subscriber.subscribe(MARKET_STREAM_CHANNEL, this::handleMarketTick);
        
        log.info("Analytics service started and subscribed to {}", MARKET_STREAM_CHANNEL);
    }
    
    void onStop(@Observes ShutdownEvent event) {
        log.info("Stopping analytics service");
        subscriber.unsubscribe(MARKET_STREAM_CHANNEL);
    }
    
    /**
     * Handle incoming market ticks
     */
    private void handleMarketTick(MarketTick tick) {
        try {
            analysisService.processTick(tick);
        } catch (Exception e) {
            log.error("Error processing tick", e);
        }
    }
    
    /**
     * Generate snapshots periodically (every 5 seconds)
     */
    @Scheduled(every = "${analytics.snapshot.interval:5s}")
    void generateSnapshots() {
        log.debug("Generating snapshots for all symbols");
        
        for (String symbol : symbols) {
            try {
                MarketSnapshot snapshot = analysisService.generateSnapshot(symbol);
                snapshotRepository.save(snapshot);
                log.debug("Saved snapshot for {}", symbol);
            } catch (Exception e) {
                log.error("Error generating snapshot for " + symbol, e);
            }
        }
    }
}
