package cl.ioio.finbot.analytics.domain;

import cl.ioio.finbot.domain.model.BayesianMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Bayesian analysis for drift and volatility estimation
 * Domain service implementing Bayesian inference
 */
@ApplicationScoped
@Slf4j
public class BayesianAnalyzer {
    
    private static final int SCALE = 8;
    
    /**
     * Calculate Bayesian metrics from price series
     * Uses conjugate prior for normal distribution
     * 
     * @param prices historical prices
     * @return Bayesian metrics with drift and volatility
     */
    public BayesianMetrics analyze(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return createDefaultMetrics();
        }
        
        try {
            // Calculate returns (log returns for better properties)
            double[] returns = calculateLogReturns(prices);
            
            if (returns.length == 0) {
                return createDefaultMetrics();
            }
            
            DescriptiveStatistics stats = new DescriptiveStatistics(returns);
            
            // Prior parameters (weakly informative)
            double priorMean = 0.0;
            double priorVariance = 0.01;
            double priorN = 1.0;
            
            // Sample statistics
            double sampleMean = stats.getMean();
            double sampleVariance = stats.getVariance();
            int sampleSize = returns.length;
            
            // Posterior parameters (Bayesian update)
            double posteriorN = priorN + sampleSize;
            double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
            
            // Posterior variance (accounting for uncertainty)
            double posteriorVariance = ((priorN * priorVariance + 
                    sampleSize * sampleVariance + 
                    (priorN * sampleSize / posteriorN) * 
                    Math.pow(sampleMean - priorMean, 2)) / posteriorN);
            
            // Calculate confidence (based on sample size and variance)
            double confidence = 1.0 - (1.0 / Math.sqrt(sampleSize + 1));
            
            // Annualize drift and volatility (assuming daily data)
            double annualizedDrift = posteriorMean * 252; // 252 trading days
            double annualizedVolatility = Math.sqrt(posteriorVariance * 252);
            
            return BayesianMetrics.builder()
                    .drift(BigDecimal.valueOf(annualizedDrift).setScale(SCALE, RoundingMode.HALF_UP))
                    .volatility(BigDecimal.valueOf(annualizedVolatility).setScale(SCALE, RoundingMode.HALF_UP))
                    .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                    .sampleSize(sampleSize)
                    .priorMean(BigDecimal.valueOf(priorMean).setScale(SCALE, RoundingMode.HALF_UP))
                    .priorVariance(BigDecimal.valueOf(priorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in Bayesian analysis", e);
            return createDefaultMetrics();
        }
    }
    
    /**
     * Calculate log returns from prices
     */
    private double[] calculateLogReturns(List<BigDecimal> prices) {
        double[] returns = new double[prices.size() - 1];
        
        for (int i = 1; i < prices.size(); i++) {
            double currentPrice = prices.get(i).doubleValue();
            double previousPrice = prices.get(i - 1).doubleValue();
            
            if (previousPrice > 0 && currentPrice > 0) {
                returns[i - 1] = Math.log(currentPrice / previousPrice);
            }
        }
        
        return returns;
    }
    
    private BayesianMetrics createDefaultMetrics() {
        return BayesianMetrics.builder()
                .drift(BigDecimal.ZERO)
                .volatility(BigDecimal.ZERO)
                .confidence(BigDecimal.ZERO)
                .sampleSize(0)
                .priorMean(BigDecimal.ZERO)
                .priorVariance(BigDecimal.valueOf(0.01))
                .build();
    }
}
