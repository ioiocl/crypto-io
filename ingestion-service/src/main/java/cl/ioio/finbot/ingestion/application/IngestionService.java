package cl.ioio.finbot.ingestion.application;

import cl.ioio.finbot.domain.model.MarketTick;
import cl.ioio.finbot.domain.ports.MarketDataIngestionPort;
import cl.ioio.finbot.domain.ports.MarketDataPublisher;
import cl.ioio.finbot.ingestion.adapter.BinanceWebSocketClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application service for market data ingestion
 * Orchestrates the ingestion process using hexagonal architecture
 */
@ApplicationScoped
@Slf4j
public class IngestionService implements MarketDataIngestionPort {
    
    private final String binanceSymbols;
    private final MarketDataPublisher publisher;
    private final AtomicBoolean active = new AtomicBoolean(false);
    
    private BinanceWebSocketClient binanceClient;
    
    @Inject
    public IngestionService(
            @ConfigProperty(name = "binance.symbols", 
                    defaultValue = "btc,eth,bnb,sol,xrp") String binanceSymbols,
            MarketDataPublisher publisher) {
        
        this.binanceSymbols = binanceSymbols;
        this.publisher = publisher;
        
        log.info("Ingestion service initialized (Crypto only)");
        log.info("Crypto symbols: {}", binanceSymbols);
    }
    
    void onStart(@Observes StartupEvent event) {
        log.info("Starting ingestion service on application startup");
        startIngestion();
    }
    
    void onStop(@Observes ShutdownEvent event) {
        log.info("Stopping ingestion service on application shutdown");
        stopIngestion();
    }
    
    @Override
    public void startIngestion(String... symbols) {
        if (active.get()) {
            log.warn("Ingestion already active");
            return;
        }
        
        // Start Binance (crypto) connection
        try {
            // Build Binance combined stream URL with all symbols
            String[] cryptoSymbols = binanceSymbols.split(",");
            StringBuilder streamUrl = new StringBuilder("wss://stream.binance.com:9443/stream?streams=");
            
            for (int i = 0; i < cryptoSymbols.length; i++) {
                if (i > 0) streamUrl.append("/");
                String symbol = cryptoSymbols[i].trim().toLowerCase();
                streamUrl.append(symbol).append("usdt@ticker");
            }
            
            log.info("Connecting to Binance: {}", streamUrl);
            URI binanceUri = new URI(streamUrl.toString());
            binanceClient = new BinanceWebSocketClient(binanceUri, this::handleMarketTick);
            
            boolean connected = binanceClient.connectBlocking();
            if (connected) {
                log.info("Successfully connected to Binance WebSocket API");
                active.set(true);
                log.info("Binance ingestion started for: {}", binanceSymbols);
            } else {
                log.error("Failed to connect to Binance WebSocket API");
            }
        } catch (Exception e) {
            log.error("Error starting Binance ingestion", e);
        }
    }
    
    @Override
    public void stopIngestion() {
        if (!active.get()) {
            log.warn("Ingestion not active");
            return;
        }
        
        try {
            if (binanceClient != null) {
                binanceClient.close();
                binanceClient = null;
                log.info("Binance client stopped");
            }
            active.set(false);
            log.info("Ingestion stopped");
        } catch (Exception e) {
            log.error("Error stopping ingestion", e);
        }
    }
    
    @Override
    public boolean isActive() {
        return active.get();
    }
    
    /**
     * Handle incoming market tick from WebSocket
     * Publishes to Redis for downstream processing
     */
    private void handleMarketTick(MarketTick tick) {
        try {
            log.debug("Received tick: {} @ {}", tick.getSymbol(), tick.getPrice());
            publisher.publish(tick);
        } catch (Exception e) {
            log.error("Error handling market tick", e);
        }
    }
}
