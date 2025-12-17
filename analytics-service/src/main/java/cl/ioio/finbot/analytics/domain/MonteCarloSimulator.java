package cl.ioio.finbot.analytics.domain;

import cl.ioio.finbot.domain.model.MonteCarloResults;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Monte Carlo simulator for risk assessment
 * Simulates future price paths using geometric Brownian motion
 */
@ApplicationScoped
@Slf4j
public class MonteCarloSimulator {
    
    private static final int SCALE = 8;
    
    @ConfigProperty(name = "monte.carlo.simulations", defaultValue = "10000")
    int defaultSimulations;
    
    @ConfigProperty(name = "monte.carlo.horizon.days", defaultValue = "7")
    int defaultHorizon;
    
    private final Random random = new Random();
    
    /**
     * Run Monte Carlo simulation
     * 
     * @param currentPrice current price
     * @param drift expected return (mu)
     * @param volatility standard deviation (sigma)
     * @param simulations number of simulations
     * @param horizon time horizon in days
     * @return Monte Carlo results with risk metrics
     */
    public MonteCarloResults simulate(
            BigDecimal currentPrice, 
            double drift, 
            double volatility,
            int simulations,
            int horizon) {
        
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return createDefaultResults(simulations);
        }
        
        try {
            double S0 = currentPrice.doubleValue();
            double dt = 1.0 / 252.0; // daily time step
            
            // Store final prices from all simulations
            double[] finalPrices = new double[simulations];
            int countUp = 0;
            int countDown = 0;
            
            NormalDistribution normalDist = new NormalDistribution(0, 1);
            
            // Run simulations
            for (int sim = 0; sim < simulations; sim++) {
                double price = S0;
                
                // Simulate price path
                for (int day = 0; day < horizon; day++) {
                    double z = normalDist.sample();
                    double dW = z * Math.sqrt(dt);
                    
                    // Geometric Brownian Motion
                    price = price * Math.exp((drift - 0.5 * volatility * volatility) * dt + 
                                            volatility * dW);
                }
                
                finalPrices[sim] = price;
                
                if (price > S0) {
                    countUp++;
                } else {
                    countDown++;
                }
            }
            
            // Calculate statistics
            Arrays.sort(finalPrices);
            DescriptiveStatistics stats = new DescriptiveStatistics(finalPrices);
            
            double expectedReturn = (stats.getMean() - S0) / S0;
            double probabilityUp = (double) countUp / simulations;
            double probabilityDown = (double) countDown / simulations;
            
            // Calculate VaR (Value at Risk)
            double var95 = S0 - finalPrices[(int)(simulations * 0.05)];
            double var99 = S0 - finalPrices[(int)(simulations * 0.01)];
            
            // Calculate CVaR (Conditional VaR / Expected Shortfall)
            double cvar = calculateCVaR(finalPrices, S0, 0.05);
            
            // Calculate percentiles
            List<MonteCarloResults.Percentile> percentiles = Arrays.asList(
                    createPercentile(5, finalPrices[(int)(simulations * 0.05)]),
                    createPercentile(25, finalPrices[(int)(simulations * 0.25)]),
                    createPercentile(50, finalPrices[(int)(simulations * 0.50)]),
                    createPercentile(75, finalPrices[(int)(simulations * 0.75)]),
                    createPercentile(95, finalPrices[(int)(simulations * 0.95)])
            );
            
            return MonteCarloResults.builder()
                    .simulations(simulations)
                    .probabilityUp(BigDecimal.valueOf(probabilityUp).setScale(SCALE, RoundingMode.HALF_UP))
                    .probabilityDown(BigDecimal.valueOf(probabilityDown).setScale(SCALE, RoundingMode.HALF_UP))
                    .expectedReturn(BigDecimal.valueOf(expectedReturn).setScale(SCALE, RoundingMode.HALF_UP))
                    .valueAtRisk95(BigDecimal.valueOf(var95).setScale(SCALE, RoundingMode.HALF_UP))
                    .valueAtRisk99(BigDecimal.valueOf(var99).setScale(SCALE, RoundingMode.HALF_UP))
                    .conditionalVaR(BigDecimal.valueOf(cvar).setScale(SCALE, RoundingMode.HALF_UP))
                    .percentiles(percentiles)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in Monte Carlo simulation", e);
            return createDefaultResults(simulations);
        }
    }
    
    /**
     * Simulate with default parameters
     */
    public MonteCarloResults simulate(BigDecimal currentPrice, double drift, double volatility) {
        return simulate(currentPrice, drift, volatility, defaultSimulations, defaultHorizon);
    }
    
    /**
     * Calculate Conditional VaR (Expected Shortfall)
     */
    private double calculateCVaR(double[] sortedPrices, double currentPrice, double alpha) {
        int cutoff = (int)(sortedPrices.length * alpha);
        double sum = 0;
        
        for (int i = 0; i < cutoff; i++) {
            sum += currentPrice - sortedPrices[i];
        }
        
        return sum / cutoff;
    }
    
    private MonteCarloResults.Percentile createPercentile(int level, double value) {
        return MonteCarloResults.Percentile.builder()
                .level(level)
                .value(BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP))
                .build();
    }
    
    private MonteCarloResults createDefaultResults(int simulations) {
        List<MonteCarloResults.Percentile> percentiles = Arrays.asList(
                createPercentile(5, 0),
                createPercentile(25, 0),
                createPercentile(50, 0),
                createPercentile(75, 0),
                createPercentile(95, 0)
        );
        
        log.info("Using Monte Carlo configuration: simulations={}, horizon={} days", 
                defaultSimulations, defaultHorizon);
        
        return MonteCarloResults.builder()
                .simulations(simulations)
                .probabilityUp(BigDecimal.valueOf(0.5))
                .probabilityDown(BigDecimal.valueOf(0.5))
                .expectedReturn(BigDecimal.ZERO)
                .valueAtRisk95(BigDecimal.ZERO)
                .valueAtRisk99(BigDecimal.ZERO)
                .conditionalVaR(BigDecimal.ZERO)
                .percentiles(percentiles)
                .build();
    }
}
