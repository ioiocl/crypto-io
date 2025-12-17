package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARIMA signal with trend detection and structural break analysis
 * Adapted from sports analytics to financial markets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ARIMASignal {
    
    @JsonProperty("trend")
    private BigDecimal trend;
    
    @JsonProperty("trendPercentage")
    private BigDecimal trendPercentage;
    
    @JsonProperty("structuralBreakDetected")
    private Boolean structuralBreakDetected;
    
    @JsonProperty("confidence")
    private BigDecimal confidence;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("cusumStatistic")
    private BigDecimal cusumStatistic;
    
    @JsonProperty("threshold")
    private BigDecimal threshold;
}
