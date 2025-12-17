package cl.ioio.finbot.analytics.application;

import cl.ioio.finbot.analytics.domain.ABCAnalyzer;
import cl.ioio.finbot.analytics.domain.ArimaForecaster;
import cl.ioio.finbot.analytics.domain.BayesianAnalyzer;
import cl.ioio.finbot.analytics.domain.MonteCarloSimulator;
import cl.ioio.finbot.domain.model.*;
import cl.ioio.finbot.domain.ports.AnalysisService;
import cl.ioio.finbot.domain.ports.SnapshotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application service for market analysis
 * Orchestrates Bayesian, ARIMA, and Monte Carlo analysis
 */
@ApplicationScoped
@Slf4j
public class MarketAnalysisService implements AnalysisService {
    
    private static final int MAX_WINDOW_SIZE = 500;
    private static final int MIN_WINDOW_SIZE = 30;
    
    private final BayesianAnalyzer bayesianAnalyzer;
    private final ArimaForecaster arimaForecaster;
    private final MonteCarloSimulator monteCarloSimulator;
    private final ABCAnalyzer abcAnalyzer;
    private final SnapshotRepository snapshotRepository;
    
    // In-memory time series windows per symbol
    private final Map<String, List<MarketTick>> tickWindows = new ConcurrentHashMap<>();
    
    @Inject
    public MarketAnalysisService(
            BayesianAnalyzer bayesianAnalyzer,
            ArimaForecaster arimaForecaster,
            MonteCarloSimulator monteCarloSimulator,
            ABCAnalyzer abcAnalyzer,
            SnapshotRepository snapshotRepository) {
        
        this.bayesianAnalyzer = bayesianAnalyzer;
        this.arimaForecaster = arimaForecaster;
        this.monteCarloSimulator = monteCarloSimulator;
        this.abcAnalyzer = abcAnalyzer;
        this.snapshotRepository = snapshotRepository;
    }
    
    @Override
    public void processTick(MarketTick tick) {
        if (tick == null || tick.getSymbol() == null || tick.getPrice() == null) {
            log.warn("Invalid tick received: {}", tick);
            return;
        }
        
        String symbol = tick.getSymbol();
        
        // Update time series window
        tickWindows.computeIfAbsent(symbol, k -> new ArrayList<>()).add(tick);
        
        // Maintain window size
        List<MarketTick> window = tickWindows.get(symbol);
        if (window.size() > MAX_WINDOW_SIZE) {
            window.remove(0);
        }
        
        log.debug("Processed tick for {}: price={}, window size={}", 
                symbol, tick.getPrice(), window.size());
    }
    
    @Override
    public MarketSnapshot generateSnapshot(String symbol) {
        List<MarketTick> window = tickWindows.get(symbol);
        
        if (window == null || window.size() < MIN_WINDOW_SIZE) {
            log.warn("Insufficient data for {}: {} ticks", symbol, 
                    window != null ? window.size() : 0);
            return createDefaultSnapshot(symbol);
        }
        
        try {
            // Extract prices
            List<BigDecimal> prices = window.stream()
                    .map(MarketTick::getPrice)
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            
            if (prices.isEmpty()) {
                return createDefaultSnapshot(symbol);
            }
            
            BigDecimal currentPrice = prices.get(prices.size() - 1);
            
            // Perform ABC (ARIMA-Bayes-Carlo) integrated analysis
            ABCAnalysisResult abcAnalysis = abcAnalyzer.analyze(prices, currentPrice);
            
            // Perform individual analyses for backward compatibility
            BayesianMetrics bayesianMetrics = bayesianAnalyzer.analyze(prices);
            ArimaForecast arimaForecast = arimaForecaster.forecast(prices);
            MonteCarloResults monteCarloResults = monteCarloSimulator.simulate(
                    currentPrice,
                    bayesianMetrics.getDrift().doubleValue(),
                    bayesianMetrics.getVolatility().doubleValue()
            );
            
            // Determine market state from ABC analysis
            String marketState = abcAnalysis.getMarketRegime();
            
            MarketSnapshot snapshot = MarketSnapshot.builder()
                    .symbol(symbol)
                    .timestamp(Instant.now())
                    .currentPrice(currentPrice)
                    .bayesianMetrics(bayesianMetrics)
                    .arimaForecast(arimaForecast)
                    .monteCarloResults(monteCarloResults)
                    .marketState(marketState)
                    .abcAnalysis(abcAnalysis)
                    .build();
            
            // Save snapshot
            snapshotRepository.save(snapshot);
            
            log.info("Generated snapshot for {}: price={}, state={}", 
                    symbol, currentPrice, marketState);
            
            return snapshot;
            
        } catch (Exception e) {
            log.error("Error generating snapshot for " + symbol, e);
            return createDefaultSnapshot(symbol);
        }
    }
    
    /**
     * Determine market state based on analytics
     */
    private String determineMarketState(
            BayesianMetrics bayesian,
            ArimaForecast arima,
            MonteCarloResults monteCarlo) {
        
        // Bullish if:
        // - Positive drift
        // - ARIMA predicts upward trend
        // - Monte Carlo shows higher probability of increase
        
        boolean positiveDrift = bayesian.getDrift().compareTo(BigDecimal.ZERO) > 0;
        
        boolean arimaUptrend = false;
        if (!arima.getPredictions().isEmpty()) {
            BigDecimal firstPrediction = arima.getPredictions().get(0);
            BigDecimal lastPrediction = arima.getPredictions().get(arima.getPredictions().size() - 1);
            arimaUptrend = lastPrediction.compareTo(firstPrediction) > 0;
        }
        
        boolean monteCarloUp = monteCarlo.getProbabilityUp()
                .compareTo(monteCarlo.getProbabilityDown()) > 0;
        
        int bullishSignals = (positiveDrift ? 1 : 0) + 
                            (arimaUptrend ? 1 : 0) + 
                            (monteCarloUp ? 1 : 0);
        
        if (bullishSignals >= 2) {
            return "BULLISH";
        } else if (bullishSignals == 0) {
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }
    
    private MarketSnapshot createDefaultSnapshot(String symbol) {
        return MarketSnapshot.builder()
                .symbol(symbol)
                .timestamp(Instant.now())
                .currentPrice(BigDecimal.ZERO)
                .bayesianMetrics(BayesianMetrics.builder().build())
                .arimaForecast(ArimaForecast.builder().build())
                .monteCarloResults(MonteCarloResults.builder().build())
                .marketState("UNKNOWN")
                .build();
    }
}
