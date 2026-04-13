package cl.ioio.finbot.aidecision;

import cl.ioio.finbot.aidecision.adapter.RedisDecisionPublisher;
import cl.ioio.finbot.aidecision.adapter.RedisMarketSnapshotReader;
import cl.ioio.finbot.aidecision.adapter.RedisNewsSignalSubscriber;
import cl.ioio.finbot.aidecision.application.DecisionEngine;
import cl.ioio.finbot.domain.model.MarketContext;
import cl.ioio.finbot.domain.model.MarketDecision;
import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.domain.model.NewsSignal;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class AiDecisionApplication {

    private static final String NEWS_CHANNEL = "news-stream";

    private final RedisNewsSignalSubscriber newsSubscriber;
    private final RedisMarketSnapshotReader snapshotReader;
    private final DecisionEngine decisionEngine;
    private final RedisDecisionPublisher decisionPublisher;

    @Inject
    public AiDecisionApplication(
            RedisNewsSignalSubscriber newsSubscriber,
            RedisMarketSnapshotReader snapshotReader,
            DecisionEngine decisionEngine,
            RedisDecisionPublisher decisionPublisher) {
        this.newsSubscriber = newsSubscriber;
        this.snapshotReader = snapshotReader;
        this.decisionEngine = decisionEngine;
        this.decisionPublisher = decisionPublisher;
    }

    void onStart(@Observes StartupEvent event) {
        newsSubscriber.subscribe(NEWS_CHANNEL, this::onNewsSignal);
        log.info("AI decision service started and subscribed to {}", NEWS_CHANNEL);
    }

    void onStop(@Observes ShutdownEvent event) {
        newsSubscriber.unsubscribe();
        log.info("AI decision service stopped");
    }

    private void onNewsSignal(NewsSignal newsSignal) {
        CompletableFuture.runAsync(() -> processNewsSignal(newsSignal));
    }

    private void processNewsSignal(NewsSignal newsSignal) {
        try {
            Optional<MarketSnapshot> snapshotOpt = snapshotReader.findLatest(newsSignal.getSymbol());
            if (snapshotOpt.isEmpty()) {
                log.warn("No market snapshot found for {}. Decision skipped.", newsSignal.getSymbol());
                return;
            }

            MarketSnapshot snapshot = snapshotOpt.get();
            MarketDecision decision = decisionEngine.decide(snapshot, newsSignal);
            MarketContext context = toMarketContext(snapshot, newsSignal, decision);

            decisionPublisher.publishDecision(decision);
            decisionPublisher.publishContext(context);

            log.info("Decision generated for {} -> {} ({})",
                    decision.getSymbol(), decision.getSignal(), decision.getConfidence());
        } catch (Exception e) {
            log.error("Error processing news signal for {}", newsSignal.getSymbol(), e);
        }
    }

    private MarketContext toMarketContext(MarketSnapshot snapshot, NewsSignal newsSignal, MarketDecision decision) {
        return MarketContext.builder()
                .symbol(snapshot.getSymbol())
                .price(snapshot.getCurrentPrice())
                .trend(snapshot.getMarketState())
                .volatility(snapshot.getBayesianMetrics() != null ? snapshot.getBayesianMetrics().getVolatility() : null)
                .newsSentiment(newsSignal.getSentimentScore())
                .newsVolume(newsSignal.getNewsVolume())
                .aiSignal(decision.getSignal())
                .aiConfidence(decision.getConfidence())
                .aiReasoning(decision.getReasoning())
                .decision(decision.getSignal())
                .timestamp(Instant.now())
                .build();
    }
}
