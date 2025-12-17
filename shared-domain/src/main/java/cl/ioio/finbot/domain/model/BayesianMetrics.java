package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Bayesian analysis metrics for volatility and drift estimation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BayesianMetrics {
    
    @JsonProperty("drift")
    private BigDecimal drift; // μ (mu) - expected return
    
    @JsonProperty("volatility")
    private BigDecimal volatility; // σ (sigma) - standard deviation
    
    @JsonProperty("confidence")
    private BigDecimal confidence; // confidence level (0-1)
    
    @JsonProperty("sampleSize")
    private Integer sampleSize;
    
    @JsonProperty("priorMean")
    private BigDecimal priorMean;
    
    @JsonProperty("priorVariance")
    private BigDecimal priorVariance;
}
