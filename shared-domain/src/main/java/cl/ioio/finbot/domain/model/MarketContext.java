package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity representing unified market context for UI and agents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketContext {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("trend")
    private String trend;

    @JsonProperty("volatility")
    private BigDecimal volatility;

    @JsonProperty("newsSentiment")
    private double newsSentiment;

    @JsonProperty("newsVolume")
    private int newsVolume;

    @JsonProperty("aiSignal")
    private String aiSignal;

    @JsonProperty("aiConfidence")
    private double aiConfidence;

    @JsonProperty("aiReasoning")
    private String aiReasoning;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("timestamp")
    private Instant timestamp;
}
