package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ABC (ARIMA-Bayes-Carlo) integrated analysis result
 * Combines three-stage analytical pipeline for comprehensive market analysis
 * 
 * Architecture:
 * Stage 1: ARIMA - Detects trends and structural breaks in price patterns
 * Stage 2: Bayesian - Updates momentum with ARIMA-informed prior
 * Stage 3: Monte Carlo - Simulates outcomes using Bayesian momentum
 * 
 * Feedback Loop: Structural breaks trigger recalibration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABCAnalysisResult {
    
    @JsonProperty("arimaSignal")
    private ARIMASignal arimaSignal;
    
    @JsonProperty("momentumMetrics")
    private MomentumMetrics momentumMetrics;
    
    @JsonProperty("marketPrediction")
    private MarketPrediction marketPrediction;
    
    @JsonProperty("abcIntegrationConfidence")
    private BigDecimal abcIntegrationConfidence;
    
    @JsonProperty("needsRecalibration")
    private Boolean needsRecalibration;
    
    @JsonProperty("marketRegime")
    private String marketRegime;
}
