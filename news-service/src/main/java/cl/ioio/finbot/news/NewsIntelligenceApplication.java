package cl.ioio.finbot.news;

import cl.ioio.finbot.domain.model.NewsSignal;
import cl.ioio.finbot.news.adapter.NewsApiClient;
import cl.ioio.finbot.news.adapter.RedisNewsSignalPublisher;
import cl.ioio.finbot.news.application.SentimentAnalyzer;
import cl.ioio.finbot.news.model.NewsArticle;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@Slf4j
public class NewsIntelligenceApplication {

    private final NewsApiClient newsApiClient;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final RedisNewsSignalPublisher publisher;
    private final List<String> symbols;
    private final int maxHeadlines;

    @Inject
    public NewsIntelligenceApplication(
            NewsApiClient newsApiClient,
            SentimentAnalyzer sentimentAnalyzer,
            RedisNewsSignalPublisher publisher,
            @ConfigProperty(name = "news.symbols", defaultValue = "BTC,ETH,BNB,SOL,XRP") String symbolsConfig,
            @ConfigProperty(name = "news.max.headlines", defaultValue = "5") int maxHeadlines) {
        this.newsApiClient = newsApiClient;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.publisher = publisher;
        this.symbols = Arrays.stream(symbolsConfig.split(",")).map(String::trim).toList();
        this.maxHeadlines = maxHeadlines;

        log.info("News service initialized for symbols: {}", symbols);
    }

    @Scheduled(every = "${news.poll.interval:5m}")
    void pollNews() {
        for (String symbol : symbols) {
            try {
                List<NewsArticle> articles = newsApiClient.fetchBySymbol(symbol, maxHeadlines);
                double score = sentimentAnalyzer.calculateScore(articles);

                NewsSignal signal = NewsSignal.builder()
                        .symbol(symbol)
                        .sentimentScore(score)
                        .newsVolume(articles.size())
                        .headlineCount(articles.size())
                        .headlines(articles.stream().map(NewsArticle::getTitle).toList())
                        .timestamp(Instant.now())
                        .build();

                publisher.publish(signal);
                log.info("News signal generated for {}: score={}, headlines={}", symbol, score, articles.size());
            } catch (Exception e) {
                log.error("Error generating news signal for {}", symbol, e);
            }
        }
    }
}
