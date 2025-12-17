package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * ARIMA model forecast results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArimaForecast {
    
    @JsonProperty("predictions")
    private List<BigDecimal> predictions; // forecasted prices
    
    @JsonProperty("confidenceIntervalLower")
    private List<BigDecimal> confidenceIntervalLower;
    
    @JsonProperty("confidenceIntervalUpper")
    private List<BigDecimal> confidenceIntervalUpper;
    
    @JsonProperty("horizon")
    private Integer horizon; // number of periods forecasted
    
    @JsonProperty("modelOrder")
    private String modelOrder; // e.g., "ARIMA(1,1,1)"
    
    @JsonProperty("aic")
    private BigDecimal aic; // Akaike Information Criterion
}
