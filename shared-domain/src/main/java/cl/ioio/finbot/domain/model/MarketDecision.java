package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain entity representing an AI-generated market decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDecision {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("signal")
    private String signal;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("reasoning")
    private String reasoning;

    @JsonProperty("timestamp")
    private Instant timestamp;
}
