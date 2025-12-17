package cl.ioio.finbot.analytics.domain;

import cl.ioio.finbot.domain.model.ArimaForecast;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ARIMA forecaster for time series prediction
 * Simplified ARIMA(1,1,1) implementation
 */
@ApplicationScoped
@Slf4j
public class ArimaForecaster {
    
    private static final int SCALE = 8;
    
    @ConfigProperty(name = "arima.horizon.periods", defaultValue = "7")
    int defaultHorizon;
    
    /**
     * Generate ARIMA forecast
     * Simplified ARIMA(1,1,1) model with exponential smoothing
     * 
     * @param prices historical prices
     * @param horizon number of periods to forecast
     * @return ARIMA forecast with predictions and confidence intervals
     */
    public ArimaForecast forecast(List<BigDecimal> prices, int horizon) {
        if (prices == null || prices.size() < 10) {
            return createDefaultForecast(horizon);
        }
        
        try {
            // Use exponential smoothing with trend (Holt's method)
            double alpha = 0.3; // level smoothing
            double beta = 0.1;  // trend smoothing
            
            double[] priceArray = prices.stream()
                    .mapToDouble(BigDecimal::doubleValue)
                    .toArray();
            
            // Initialize level and trend
            double level = priceArray[0];
            double trend = (priceArray[priceArray.length - 1] - priceArray[0]) / priceArray.length;
            
            // Smooth the series
            for (int i = 1; i < priceArray.length; i++) {
                double prevLevel = level;
                level = alpha * priceArray[i] + (1 - alpha) * (level + trend);
                trend = beta * (level - prevLevel) + (1 - beta) * trend;
            }
            
            // Generate forecasts
            List<BigDecimal> predictions = new ArrayList<>();
            List<BigDecimal> lowerBounds = new ArrayList<>();
            List<BigDecimal> upperBounds = new ArrayList<>();
            
            // Calculate standard error
            DescriptiveStatistics stats = new DescriptiveStatistics(priceArray);
            double stdError = stats.getStandardDeviation();
            
            for (int h = 1; h <= horizon; h++) {
                double forecast = level + h * trend;
                double margin = 1.96 * stdError * Math.sqrt(h); // 95% confidence
                
                predictions.add(BigDecimal.valueOf(forecast).setScale(SCALE, RoundingMode.HALF_UP));
                lowerBounds.add(BigDecimal.valueOf(forecast - margin).setScale(SCALE, RoundingMode.HALF_UP));
                upperBounds.add(BigDecimal.valueOf(forecast + margin).setScale(SCALE, RoundingMode.HALF_UP));
            }
            
            // Calculate AIC (simplified)
            double aic = calculateAIC(priceArray, 3); // 3 parameters: level, trend, error
            
            return ArimaForecast.builder()
                    .predictions(predictions)
                    .confidenceIntervalLower(lowerBounds)
                    .confidenceIntervalUpper(upperBounds)
                    .horizon(horizon)
                    .modelOrder("ARIMA(1,1,1)")
                    .aic(BigDecimal.valueOf(aic).setScale(SCALE, RoundingMode.HALF_UP))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in ARIMA forecasting", e);
            return createDefaultForecast(horizon);
        }
    }
    
    /**
     * Forecast with default horizon
     */
    public ArimaForecast forecast(List<BigDecimal> prices) {
        log.debug("Using ARIMA configuration: horizon={} periods", defaultHorizon);
        return forecast(prices, defaultHorizon);
    }
    
    /**
     * Calculate Akaike Information Criterion
     */
    private double calculateAIC(double[] data, int numParams) {
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double variance = stats.getVariance();
        int n = data.length;
        
        if (variance <= 0 || n <= numParams) {
            return Double.MAX_VALUE;
        }
        
        return n * Math.log(variance) + 2 * numParams;
    }
    
    private ArimaForecast createDefaultForecast(int horizon) {
        List<BigDecimal> zeros = new ArrayList<>();
        for (int i = 0; i < horizon; i++) {
            zeros.add(BigDecimal.ZERO);
        }
        
        return ArimaForecast.builder()
                .predictions(zeros)
                .confidenceIntervalLower(zeros)
                .confidenceIntervalUpper(zeros)
                .horizon(horizon)
                .modelOrder("ARIMA(0,0,0)")
                .aic(BigDecimal.ZERO)
                .build();
    }
}
