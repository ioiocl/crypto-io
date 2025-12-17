package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Momentum metrics from Bayesian analysis with ARIMA-informed priors
 * Represents market momentum (drift) and uncertainty (volatility)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentumMetrics {
    
    @JsonProperty("drift")
    private BigDecimal drift;
    
    @JsonProperty("volatility")
    private BigDecimal volatility;
    
    @JsonProperty("confidence")
    private BigDecimal confidence;
    
    @JsonProperty("priorMean")
    private BigDecimal priorMean;
    
    @JsonProperty("posteriorMean")
    private BigDecimal posteriorMean;
    
    @JsonProperty("priorVariance")
    private BigDecimal priorVariance;
    
    @JsonProperty("posteriorVariance")
    private BigDecimal posteriorVariance;
}
