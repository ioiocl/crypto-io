package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain entity representing the complete market state snapshot
 * Combines current price with analytical results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSnapshot {
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("currentPrice")
    private BigDecimal currentPrice;
    
    @JsonProperty("bayesianMetrics")
    private BayesianMetrics bayesianMetrics;
    
    @JsonProperty("arimaForecast")
    private ArimaForecast arimaForecast;
    
    @JsonProperty("monteCarloResults")
    private MonteCarloResults monteCarloResults;
    
    @JsonProperty("marketState")
    private String marketState; // BULLISH, BEARISH, NEUTRAL
    
    @JsonProperty("abcAnalysis")
    private ABCAnalysisResult abcAnalysis;
}
