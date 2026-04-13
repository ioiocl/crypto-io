package cl.ioio.finbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain entity representing aggregated news intelligence for a symbol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSignal {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("sentimentScore")
    private double sentimentScore;

    @JsonProperty("newsVolume")
    private int newsVolume;

    @JsonProperty("headlineCount")
    private int headlineCount;

    @JsonProperty("headlines")
    private List<String> headlines;

    @JsonProperty("timestamp")
    private Instant timestamp;
}
