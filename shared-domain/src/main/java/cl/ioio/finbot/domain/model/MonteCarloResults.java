package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monte Carlo simulation results for risk assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonteCarloResults {
    
    @JsonProperty("simulations")
    private Integer simulations; // number of simulations run
    
    @JsonProperty("probabilityUp")
    private BigDecimal probabilityUp; // probability of price increase
    
    @JsonProperty("probabilityDown")
    private BigDecimal probabilityDown; // probability of price decrease
    
    @JsonProperty("expectedReturn")
    private BigDecimal expectedReturn;
    
    @JsonProperty("valueAtRisk95")
    private BigDecimal valueAtRisk95; // VaR at 95% confidence
    
    @JsonProperty("valueAtRisk99")
    private BigDecimal valueAtRisk99; // VaR at 99% confidence
    
    @JsonProperty("conditionalVaR")
    private BigDecimal conditionalVaR; // CVaR (Expected Shortfall)
    
    @JsonProperty("percentiles")
    private List<Percentile> percentiles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Percentile {
        @JsonProperty("level")
        private Integer level; // e.g., 5, 25, 50, 75, 95
        
        @JsonProperty("value")
        private BigDecimal value;
    }
}
