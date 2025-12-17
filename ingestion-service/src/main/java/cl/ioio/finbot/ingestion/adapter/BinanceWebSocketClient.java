package cl.ioio.finbot.ingestion.adapter;

import cl.ioio.finbot.domain.model.MarketTick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * WebSocket client adapter for Binance API
 * Provides 24/7 crypto market data
 */
@Slf4j
public class BinanceWebSocketClient extends WebSocketClient {
    
    private final Consumer<MarketTick> tickHandler;
    private final ObjectMapper objectMapper;
    
    public BinanceWebSocketClient(URI serverUri, Consumer<MarketTick> tickHandler) {
        super(serverUri);
        this.tickHandler = tickHandler;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Configure SSL
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new java.security.SecureRandom());
            
            this.setSocketFactory(sslContext.getSocketFactory());
            log.debug("SSL context configured for Binance WebSocket");
        } catch (Exception e) {
            log.warn("Failed to configure SSL context: {}", e.getMessage());
        }
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to Binance WebSocket API");
    }
    
    @Override
    public void onMessage(String message) {
        try {
            log.debug("Received Binance message: {}", message);
            
            JsonNode root = objectMapper.readTree(message);
            
            // Handle combined stream format: {"stream":"btcusdt@ticker","data":{...}}
            if (root.has("stream") && root.has("data")) {
                JsonNode event = root.get("data");
                
                // Handle ticker events (24hr ticker)
                if (event.has("e") && "24hrTicker".equals(event.get("e").asText())) {
                    handleTickerMessage(event);
                }
            }
            // Handle direct stream format
            else if (root.has("e")) {
                String eventType = root.get("e").asText();
                
                if ("24hrTicker".equals(eventType)) {
                    handleTickerMessage(root);
                } else if ("trade".equals(eventType)) {
                    handleTradeMessage(root);
                } else if ("kline".equals(eventType)) {
                    handleKlineMessage(root);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing Binance message: {}", message, e);
        }
    }
    
    private void handleTickerMessage(JsonNode event) {
        try {
            String symbol = event.get("s").asText(); // e.g., "BTCUSDT"
            String cleanSymbol = cleanSymbol(symbol);
            
            BigDecimal lastPrice = new BigDecimal(event.get("c").asText());
            BigDecimal volume = new BigDecimal(event.get("v").asText());
            BigDecimal priceChange = new BigDecimal(event.get("p").asText());
            BigDecimal priceChangePercent = new BigDecimal(event.get("P").asText());
            BigDecimal high = new BigDecimal(event.get("h").asText());
            BigDecimal low = new BigDecimal(event.get("l").asText());
            BigDecimal open = new BigDecimal(event.get("o").asText());
            
            long timestamp = event.get("E").asLong(); // Event time
            
            MarketTick tick = MarketTick.builder()
                    .symbol(cleanSymbol)
                    .price(lastPrice)
                    .volume(volume.longValue())
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .open(open)
                    .high(high)
                    .low(low)
                    .exchange("BINANCE")
                    .build();
            
            tickHandler.accept(tick);
            log.debug("Processed Binance ticker for {}: price={}, volume={}", cleanSymbol, lastPrice, volume);
            
        } catch (Exception e) {
            log.error("Error handling Binance ticker message: {}", event, e);
        }
    }
    
    private void handleTradeMessage(JsonNode event) {
        try {
            String symbol = event.get("s").asText();
            String cleanSymbol = cleanSymbol(symbol);
            
            BigDecimal price = new BigDecimal(event.get("p").asText());
            BigDecimal quantity = new BigDecimal(event.get("q").asText());
            long timestamp = event.get("T").asLong();
            
            MarketTick tick = MarketTick.builder()
                    .symbol(cleanSymbol)
                    .price(price)
                    .volume(quantity.longValue())
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .exchange("BINANCE")
                    .build();
            
            tickHandler.accept(tick);
            log.debug("Processed Binance trade for {}: price={}", cleanSymbol, price);
            
        } catch (Exception e) {
            log.error("Error handling Binance trade message: {}", event, e);
        }
    }
    
    private void handleKlineMessage(JsonNode event) {
        try {
            String symbol = event.get("s").asText();
            String cleanSymbol = cleanSymbol(symbol);
            
            JsonNode kline = event.get("k");
            BigDecimal close = new BigDecimal(kline.get("c").asText());
            BigDecimal open = new BigDecimal(kline.get("o").asText());
            BigDecimal high = new BigDecimal(kline.get("h").asText());
            BigDecimal low = new BigDecimal(kline.get("l").asText());
            BigDecimal volume = new BigDecimal(kline.get("v").asText());
            long timestamp = kline.get("T").asLong();
            
            MarketTick tick = MarketTick.builder()
                    .symbol(cleanSymbol)
                    .price(close)
                    .volume(volume.longValue())
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .open(open)
                    .high(high)
                    .low(low)
                    .exchange("BINANCE")
                    .build();
            
            tickHandler.accept(tick);
            log.debug("Processed Binance kline for {}: close={}", cleanSymbol, close);
            
        } catch (Exception e) {
            log.error("Error handling Binance kline message: {}", event, e);
        }
    }
    
    /**
     * Clean symbol name: BTCUSDT -> BTC
     */
    private String cleanSymbol(String symbol) {
        if (symbol.endsWith("USDT")) {
            return symbol.substring(0, symbol.length() - 4);
        }
        if (symbol.endsWith("BUSD")) {
            return symbol.substring(0, symbol.length() - 4);
        }
        return symbol;
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("Binance WebSocket closed: code={}, reason={}, remote={}", code, reason, remote);
    }
    
    @Override
    public void onError(Exception ex) {
        log.error("Binance WebSocket error occurred", ex);
    }
    
    /**
     * Subscribe to multiple crypto pairs
     * Top 5: BTC, ETH, BNB, SOL, XRP
     */
    public void subscribeToSymbols(String... symbols) {
        if (!isOpen()) {
            log.warn("Cannot subscribe - Binance connection not open");
            return;
        }
        
        // Binance uses combined stream format
        // We'll subscribe to ticker streams for real-time updates
        StringBuilder streams = new StringBuilder();
        streams.append("{\"method\":\"SUBSCRIBE\",\"params\":[");
        
        for (int i = 0; i < symbols.length; i++) {
            if (i > 0) streams.append(",");
            String pair = symbols[i].toLowerCase() + "usdt";
            // Subscribe to ticker (24hr rolling window)
            streams.append("\"").append(pair).append("@ticker\"");
        }
        
        streams.append("],\"id\":1}");
        
        send(streams.toString());
        log.info("Subscribed to Binance symbols: {}", String.join(", ", symbols));
    }
}
