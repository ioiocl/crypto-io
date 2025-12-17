package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Market prediction from Monte Carlo simulation
 * Provides probability distributions for price movements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPrediction {
    
    @JsonProperty("probabilityUp")
    private BigDecimal probabilityUp;
    
    @JsonProperty("probabilityDown")
    private BigDecimal probabilityDown;
    
    @JsonProperty("probabilityNeutral")
    private BigDecimal probabilityNeutral;
    
    @JsonProperty("expectedPriceChange")
    private BigDecimal expectedPriceChange;
    
    @JsonProperty("expectedPriceChangePercent")
    private BigDecimal expectedPriceChangePercent;
    
    @JsonProperty("mostLikelyScenario")
    private String mostLikelyScenario;
    
    @JsonProperty("priceTargets")
    private List<PriceTarget> priceTargets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceTarget {
        @JsonProperty("percentile")
        private Integer percentile;
        
        @JsonProperty("price")
        private BigDecimal price;
        
        @JsonProperty("changePercent")
        private BigDecimal changePercent;
    }
}
