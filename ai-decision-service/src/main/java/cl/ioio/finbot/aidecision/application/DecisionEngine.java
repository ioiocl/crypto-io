package cl.ioio.finbot.aidecision.application;

import cl.ioio.finbot.aidecision.adapter.OpenAiDecisionClient;
import cl.ioio.finbot.domain.model.MarketDecision;
import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.model.NewsSignal;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class DecisionEngine {

    private final OpenAiDecisionClient openAiClient;
    private final double confidenceFloor;

    @Inject
    public DecisionEngine(
            OpenAiDecisionClient openAiClient,
            @ConfigProperty(name = "decision.confidence.floor", defaultValue = "0.55") double confidenceFloor) {
        this.openAiClient = openAiClient;
        this.confidenceFloor = confidenceFloor;
    }

    public MarketDecision decide(MarketSnapshot snapshot, NewsSignal newsSignal) {
        Optional<JsonNode> aiOutput = openAiClient.getDecisionJson(buildPrompt(snapshot, newsSignal));
        if (aiOutput.isPresent()) {
            MarketDecision parsed = parseAiDecision(snapshot.getSymbol(), aiOutput.get());
            if (parsed.getConfidence() < confidenceFloor) {
                return MarketDecision.builder()
                        .symbol(parsed.getSymbol())
                        .signal("HOLD")
                        .confidence(parsed.getConfidence())
                        .reasoning(parsed.getReasoning() + " | confidence below threshold")
                        .timestamp(parsed.getTimestamp())
                        .build();
            }
            return parsed;
        }

        return heuristicDecision(snapshot, newsSignal);
    }

    private String buildPrompt(MarketSnapshot snapshot, NewsSignal newsSignal) {
        return "Evaluate the current market state and determine a trading signal. " +
                "Return JSON with keys signal, confidence, reasoning. " +
                "Data: symbol=" + snapshot.getSymbol() +
                ", price=" + snapshot.getCurrentPrice() +
                ", trend=" + snapshot.getMarketState() +
                ", volatility=" + (snapshot.getBayesianMetrics() != null ? snapshot.getBayesianMetrics().getVolatility() : "0") +
                ", sentiment=" + newsSignal.getSentimentScore() +
                ", newsVolume=" + newsSignal.getNewsVolume();
    }

    private MarketDecision parseAiDecision(String symbol, JsonNode jsonNode) {
        String signal = jsonNode.path("signal").asText("HOLD").toUpperCase();
        double confidence = clamp(jsonNode.path("confidence").asDouble(0.5));
        String reasoning = jsonNode.path("reasoning").asText("AI reasoning unavailable");

        return MarketDecision.builder()
                .symbol(symbol)
                .signal(normalizeSignal(signal))
                .confidence(confidence)
                .reasoning(reasoning)
                .timestamp(Instant.now())
                .build();
    }

    private MarketDecision heuristicDecision(MarketSnapshot snapshot, NewsSignal newsSignal) {
        String trend = snapshot.getMarketState() == null ? "UNKNOWN" : snapshot.getMarketState();
        double sentiment = newsSignal.getSentimentScore();

        String signal;
        double confidence;
        String reasoning;

        if ("BULLISH".equalsIgnoreCase(trend) && sentiment >= -0.2) {
            signal = "BUY";
            confidence = 0.62;
            reasoning = "Fallback: bullish market state with non-negative news pressure";
        } else if ("BEARISH".equalsIgnoreCase(trend) && sentiment < 0.0) {
            signal = "SELL";
            confidence = 0.61;
            reasoning = "Fallback: bearish market state reinforced by negative sentiment";
        } else {
            signal = "HOLD";
            confidence = 0.58;
            reasoning = "Fallback: mixed market and news signals";
        }

        if (confidence < confidenceFloor) {
            signal = "HOLD";
            reasoning = reasoning + " | confidence below threshold";
        }

        return MarketDecision.builder()
                .symbol(snapshot.getSymbol())
                .signal(signal)
                .confidence(clamp(confidence))
                .reasoning(reasoning)
                .timestamp(Instant.now())
                .build();
    }

    private String normalizeSignal(String signal) {
        return switch (signal) {
            case "BUY", "SELL", "HOLD" -> signal;
            default -> "HOLD";
        };
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
