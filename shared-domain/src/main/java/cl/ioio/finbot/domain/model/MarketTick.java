package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity representing a market tick (price update)
 * Technology-agnostic, pure domain model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketTick {
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("volume")
    private Long volume;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("exchange")
    private String exchange;
    
    @JsonProperty("bid")
    private BigDecimal bid;
    
    @JsonProperty("ask")
    private BigDecimal ask;
    
    @JsonProperty("high")
    private BigDecimal high;
    
    @JsonProperty("low")
    private BigDecimal low;
    
    @JsonProperty("open")
    private BigDecimal open;
}
