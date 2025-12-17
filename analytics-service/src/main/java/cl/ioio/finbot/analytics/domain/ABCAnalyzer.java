package cl.ioio.finbot.analytics.domain;

import cl.ioio.finbot.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ABC (ARIMA-Bayes-Carlo) Integrated Analyzer
 * 
 * Three-stage analytical pipeline adapted for financial markets:
 * 
 * Stage 1: ARIMA Analysis
 *   - Detects trends in price patterns
 *   - Identifies structural breaks (CUSUM algorithm)
 *   - Exports trend signals
 * 
 * Stage 2: Bayesian Momentum Analysis
 *   - Updates momentum with ARIMA-informed prior
 *   - Calculates drift (momentum) and volatility
 *   - Adjusts confidence based on structural breaks
 * 
 * Stage 3: Monte Carlo Simulation
 *   - Simulates thousands of price paths
 *   - Uses Bayesian momentum to adjust probabilities
 *   - Generates probability distributions
 * 
 * Feedback Loop: Recalibration Trigger
 *   - Structural break detected → reset models
 *   - High volatility → reduce confidence
 */
@ApplicationScoped
@Slf4j
public class ABCAnalyzer {
    
    private static final int SCALE = 8;
    private static final double CUSUM_THRESHOLD_MULTIPLIER = 3.0;
    private static final double HIGH_VOLATILITY_THRESHOLD = 0.50;
    private static final double STRUCTURAL_BREAK_PENALTY = 0.7;
    
    private final ArimaForecaster arimaForecaster;
    private final BayesianAnalyzer bayesianAnalyzer;
    private final MonteCarloSimulator monteCarloSimulator;
    
    @Inject
    public ABCAnalyzer(
            ArimaForecaster arimaForecaster,
            BayesianAnalyzer bayesianAnalyzer,
            MonteCarloSimulator monteCarloSimulator) {
        this.arimaForecaster = arimaForecaster;
        this.bayesianAnalyzer = bayesianAnalyzer;
        this.monteCarloSimulator = monteCarloSimulator;
    }
    
    /**
     * Perform integrated ABC analysis
     * 
     * @param prices historical price series
     * @param currentPrice current market price
     * @return complete ABC analysis result
     */
    public ABCAnalysisResult analyze(List<BigDecimal> prices, BigDecimal currentPrice) {
        if (prices == null || prices.isEmpty() || currentPrice == null) {
            return createDefaultResult();
        }
        
        try {
            // Stage 1: ARIMA Analysis with structural break detection
            ARIMASignal arimaSignal = performARIMAAnalysis(prices);
            
            // Stage 2: Bayesian Momentum Analysis with ARIMA-informed prior
            MomentumMetrics momentumMetrics = performBayesianMomentumAnalysis(
                    prices, arimaSignal);
            
            // Stage 3: Monte Carlo Simulation with Bayesian momentum
            MarketPrediction marketPrediction = performMonteCarloSimulation(
                    currentPrice, momentumMetrics);
            
            // Calculate integration confidence
            double integrationConfidence = calculateIntegrationConfidence(
                    arimaSignal, momentumMetrics);
            
            // Determine if recalibration is needed
            boolean needsRecalibration = arimaSignal.getStructuralBreakDetected() ||
                    momentumMetrics.getVolatility().doubleValue() > HIGH_VOLATILITY_THRESHOLD;
            
            // Determine market regime
            String marketRegime = determineMarketRegime(
                    arimaSignal, momentumMetrics, marketPrediction);
            
            ABCAnalysisResult result = ABCAnalysisResult.builder()
                    .arimaSignal(arimaSignal)
                    .momentumMetrics(momentumMetrics)
                    .marketPrediction(marketPrediction)
                    .abcIntegrationConfidence(
                            BigDecimal.valueOf(integrationConfidence)
                                    .setScale(SCALE, RoundingMode.HALF_UP))
                    .needsRecalibration(needsRecalibration)
                    .marketRegime(marketRegime)
                    .build();
            
            logAnalysisResult(result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error in ABC analysis", e);
            return createDefaultResult();
        }
    }
    
    /**
     * Stage 1: ARIMA Analysis with trend detection and structural breaks
     */
    private ARIMASignal performARIMAAnalysis(List<BigDecimal> prices) {
        double[] priceArray = prices.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
        
        DescriptiveStatistics stats = new DescriptiveStatistics(priceArray);
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        
        double trend = calculateTrend(priceArray);
        double trendPercentage = (trend / mean) * 100.0;
        
        double cusumStatistic = detectStructuralBreak(priceArray, mean, stdDev);
        double threshold = CUSUM_THRESHOLD_MULTIPLIER * stdDev;
        boolean structuralBreak = Math.abs(cusumStatistic) > threshold;
        
        double confidence = calculateARIMAConfidence(priceArray, structuralBreak);
        
        String description = buildARIMADescription(trendPercentage, structuralBreak);
        
        return ARIMASignal.builder()
                .trend(BigDecimal.valueOf(trend).setScale(SCALE, RoundingMode.HALF_UP))
                .trendPercentage(BigDecimal.valueOf(trendPercentage).setScale(2, RoundingMode.HALF_UP))
                .structuralBreakDetected(structuralBreak)
                .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                .description(description)
                .cusumStatistic(BigDecimal.valueOf(cusumStatistic).setScale(SCALE, RoundingMode.HALF_UP))
                .threshold(BigDecimal.valueOf(threshold).setScale(SCALE, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * Calculate trend using Holt's exponential smoothing
     */
    private double calculateTrend(double[] prices) {
        if (prices.length < 2) {
            return 0.0;
        }
        
        double alpha = 0.3;
        double beta = 0.1;
        
        double level = prices[0];
        double trend = (prices[prices.length - 1] - prices[0]) / prices.length;
        
        for (int i = 1; i < prices.length; i++) {
            double prevLevel = level;
            level = alpha * prices[i] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }
        
        return trend;
    }
    
    /**
     * Detect structural breaks using CUSUM (Cumulative Sum Control Chart)
     * Monitors last 30% of observations for sudden changes
     */
    private double detectStructuralBreak(double[] prices, double mean, double stdDev) {
        if (prices.length < 10 || stdDev == 0) {
            return 0.0;
        }
        
        int monitorStart = (int)(prices.length * 0.7);
        double cusum = 0.0;
        double maxCusum = 0.0;
        
        for (int i = monitorStart; i < prices.length; i++) {
            double deviation = (prices[i] - mean) / stdDev;
            cusum += deviation;
            maxCusum = Math.max(Math.abs(maxCusum), Math.abs(cusum));
        }
        
        return maxCusum;
    }
    
    /**
     * Calculate ARIMA confidence based on data quality and stability
     */
    private double calculateARIMAConfidence(double[] prices, boolean structuralBreak) {
        double baseConfidence = 1.0 - (1.0 / Math.sqrt(prices.length + 1));
        
        if (structuralBreak) {
            baseConfidence *= STRUCTURAL_BREAK_PENALTY;
        }
        
        return Math.max(0.0, Math.min(1.0, baseConfidence));
    }
    
    /**
     * Build human-readable ARIMA description
     */
    private String buildARIMADescription(double trendPercentage, boolean structuralBreak) {
        String direction;
        String breakSuffix = structuralBreak ? " [STRUCTURAL BREAK DETECTED]" : "";
        
        if (Math.abs(trendPercentage) < 1.0) {
            return "Price stable" + breakSuffix;
        } else if (trendPercentage > 0) {
            direction = String.format("Price increasing %.2f%% in trend", trendPercentage);
        } else {
            direction = String.format("Price decreasing %.2f%% in trend", Math.abs(trendPercentage));
        }
        
        return direction + breakSuffix;
    }
    
    /**
     * Stage 2: Bayesian Momentum Analysis with ARIMA-informed prior
     */
    private MomentumMetrics performBayesianMomentumAnalysis(
            List<BigDecimal> prices, ARIMASignal arimaSignal) {
        
        double[] returns = calculateLogReturns(prices);
        
        if (returns.length == 0) {
            return createDefaultMomentumMetrics();
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics(returns);
        
        double arimaTrend = arimaSignal.getTrend().doubleValue();
        double arimaConfidence = arimaSignal.getConfidence().doubleValue();
        
        double priorMean = arimaTrend * 10.0;
        double priorVariance = 0.01 * (2.0 - arimaConfidence);
        double priorN = 1.0 + arimaConfidence;
        
        double sampleMean = stats.getMean();
        double sampleVariance = stats.getVariance();
        int sampleSize = returns.length;
        
        double posteriorN = priorN + sampleSize;
        double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
        
        double posteriorVariance = ((priorN * priorVariance + 
                sampleSize * sampleVariance + 
                (priorN * sampleSize / posteriorN) * 
                Math.pow(sampleMean - priorMean, 2)) / posteriorN);
        
        double confidence = 1.0 - (1.0 / Math.sqrt(sampleSize + 1));
        
        if (arimaSignal.getStructuralBreakDetected()) {
            confidence *= STRUCTURAL_BREAK_PENALTY;
        }
        
        double annualizedDrift = posteriorMean * 252;
        double annualizedVolatility = Math.sqrt(posteriorVariance * 252);
        
        return MomentumMetrics.builder()
                .drift(BigDecimal.valueOf(annualizedDrift).setScale(SCALE, RoundingMode.HALF_UP))
                .volatility(BigDecimal.valueOf(annualizedVolatility).setScale(SCALE, RoundingMode.HALF_UP))
                .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                .priorMean(BigDecimal.valueOf(priorMean).setScale(SCALE, RoundingMode.HALF_UP))
                .posteriorMean(BigDecimal.valueOf(posteriorMean).setScale(SCALE, RoundingMode.HALF_UP))
                .priorVariance(BigDecimal.valueOf(priorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                .posteriorVariance(BigDecimal.valueOf(posteriorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                .build();
    }
    
    /**
     * Stage 3: Monte Carlo Simulation with Bayesian momentum
     */
    private MarketPrediction performMonteCarloSimulation(
            BigDecimal currentPrice, MomentumMetrics momentum) {
        
        double drift = momentum.getDrift().doubleValue();
        double volatility = momentum.getVolatility().doubleValue();
        
        MonteCarloResults mcResults = monteCarloSimulator.simulate(
                currentPrice, drift, volatility);
        
        double probUp = mcResults.getProbabilityUp().doubleValue();
        double probDown = mcResults.getProbabilityDown().doubleValue();
        double probNeutral = 1.0 - probUp - probDown;
        
        double expectedReturn = mcResults.getExpectedReturn().doubleValue();
        double expectedPriceChange = currentPrice.doubleValue() * expectedReturn;
        double expectedPriceChangePercent = expectedReturn * 100.0;
        
        String mostLikelyScenario = determineMostLikelyScenario(probUp, probDown, probNeutral);
        
        List<MarketPrediction.PriceTarget> priceTargets = new ArrayList<>();
        for (MonteCarloResults.Percentile p : mcResults.getPercentiles()) {
            double changePercent = ((p.getValue().doubleValue() - currentPrice.doubleValue()) 
                    / currentPrice.doubleValue()) * 100.0;
            
            priceTargets.add(MarketPrediction.PriceTarget.builder()
                    .percentile(p.getLevel())
                    .price(p.getValue())
                    .changePercent(BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        
        return MarketPrediction.builder()
                .probabilityUp(BigDecimal.valueOf(probUp).setScale(SCALE, RoundingMode.HALF_UP))
                .probabilityDown(BigDecimal.valueOf(probDown).setScale(SCALE, RoundingMode.HALF_UP))
                .probabilityNeutral(BigDecimal.valueOf(Math.max(0, probNeutral)).setScale(SCALE, RoundingMode.HALF_UP))
                .expectedPriceChange(BigDecimal.valueOf(expectedPriceChange).setScale(2, RoundingMode.HALF_UP))
                .expectedPriceChangePercent(BigDecimal.valueOf(expectedPriceChangePercent).setScale(2, RoundingMode.HALF_UP))
                .mostLikelyScenario(mostLikelyScenario)
                .priceTargets(priceTargets)
                .build();
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
    
    /**
     * Calculate integration confidence using geometric mean with stability adjustment
     */
    private double calculateIntegrationConfidence(
            ARIMASignal arimaSignal, MomentumMetrics momentum) {
        
        double arimaConf = arimaSignal.getConfidence().doubleValue();
        double bayesConf = momentum.getConfidence().doubleValue();
        
        double stabilityFactor = arimaSignal.getStructuralBreakDetected() 
                ? STRUCTURAL_BREAK_PENALTY : 1.0;
        
        double geometricMean = Math.sqrt(arimaConf * bayesConf);
        
        return geometricMean * stabilityFactor;
    }
    
    /**
     * Determine market regime based on integrated signals
     */
    private String determineMarketRegime(
            ARIMASignal arima, MomentumMetrics momentum, MarketPrediction prediction) {
        
        double trendPct = arima.getTrendPercentage().doubleValue();
        double drift = momentum.getDrift().doubleValue();
        double volatility = momentum.getVolatility().doubleValue();
        double probUp = prediction.getProbabilityUp().doubleValue();
        
        if (arima.getStructuralBreakDetected()) {
            return "REGIME_CHANGE";
        }
        
        if (volatility > HIGH_VOLATILITY_THRESHOLD) {
            return "HIGH_VOLATILITY";
        }
        
        int bullishSignals = 0;
        if (trendPct > 2.0) bullishSignals++;
        if (drift > 0.05) bullishSignals++;
        if (probUp > 0.6) bullishSignals++;
        
        if (bullishSignals >= 2) {
            return volatility > 0.30 ? "BULLISH_VOLATILE" : "BULLISH_STABLE";
        }
        
        int bearishSignals = 0;
        if (trendPct < -2.0) bearishSignals++;
        if (drift < -0.05) bearishSignals++;
        if (probUp < 0.4) bearishSignals++;
        
        if (bearishSignals >= 2) {
            return volatility > 0.30 ? "BEARISH_VOLATILE" : "BEARISH_STABLE";
        }
        
        return volatility > 0.30 ? "NEUTRAL_VOLATILE" : "NEUTRAL_STABLE";
    }
    
    /**
     * Determine most likely scenario
     */
    private String determineMostLikelyScenario(double probUp, double probDown, double probNeutral) {
        if (probUp > probDown && probUp > probNeutral) {
            return "UPWARD_MOVEMENT";
        } else if (probDown > probUp && probDown > probNeutral) {
            return "DOWNWARD_MOVEMENT";
        } else {
            return "SIDEWAYS_MOVEMENT";
        }
    }
    
    /**
     * Log analysis result for monitoring
     */
    private void logAnalysisResult(ABCAnalysisResult result) {
        String status = result.getNeedsRecalibration() ? "⚠️ RECALIBRATION NEEDED!" : "✓";
        
        log.info("{} ABC Analysis - Regime: {} - Confidence: {} - ARIMA: {}", 
                status,
                result.getMarketRegime(),
                result.getAbcIntegrationConfidence(),
                result.getArimaSignal().getDescription());
    }
    
    private ABCAnalysisResult createDefaultResult() {
        return ABCAnalysisResult.builder()
                .arimaSignal(createDefaultARIMASignal())
                .momentumMetrics(createDefaultMomentumMetrics())
                .marketPrediction(createDefaultMarketPrediction())
                .abcIntegrationConfidence(BigDecimal.ZERO)
                .needsRecalibration(false)
                .marketRegime("UNKNOWN")
                .build();
    }
    
    private ARIMASignal createDefaultARIMASignal() {
        return ARIMASignal.builder()
                .trend(BigDecimal.ZERO)
                .trendPercentage(BigDecimal.ZERO)
                .structuralBreakDetected(false)
                .confidence(BigDecimal.ZERO)
                .description("Insufficient data")
                .cusumStatistic(BigDecimal.ZERO)
                .threshold(BigDecimal.ZERO)
                .build();
    }
    
    private MomentumMetrics createDefaultMomentumMetrics() {
        return MomentumMetrics.builder()
                .drift(BigDecimal.ZERO)
                .volatility(BigDecimal.ZERO)
                .confidence(BigDecimal.ZERO)
                .priorMean(BigDecimal.ZERO)
                .posteriorMean(BigDecimal.ZERO)
                .priorVariance(BigDecimal.valueOf(0.01))
                .posteriorVariance(BigDecimal.ZERO)
                .build();
    }
    
    private MarketPrediction createDefaultMarketPrediction() {
        return MarketPrediction.builder()
                .probabilityUp(BigDecimal.valueOf(0.5))
                .probabilityDown(BigDecimal.valueOf(0.5))
                .probabilityNeutral(BigDecimal.ZERO)
                .expectedPriceChange(BigDecimal.ZERO)
                .expectedPriceChangePercent(BigDecimal.ZERO)
                .mostLikelyScenario("UNKNOWN")
                .priceTargets(new ArrayList<>())
                .build();
    }
}
