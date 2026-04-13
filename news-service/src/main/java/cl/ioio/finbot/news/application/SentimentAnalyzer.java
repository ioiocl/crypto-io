package cl.ioio.finbot.news.application;

import cl.ioio.finbot.news.model.NewsArticle;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SentimentAnalyzer {

    private static final Set<String> POSITIVE_WORDS = Set.of(
            "up", "surge", "rally", "gain", "bullish", "breakout", "strong", "record", "growth", "optimism"
    );

    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "down", "drop", "selloff", "loss", "bearish", "crash", "weak", "fear", "risk", "decline"
    );

    public double calculateScore(List<NewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return 0.0;
        }

        int score = 0;
        int totalTerms = 0;

        for (NewsArticle article : articles) {
            String content = ((article.getTitle() == null ? "" : article.getTitle()) + " " +
                    (article.getDescription() == null ? "" : article.getDescription())).toLowerCase();

            for (String positive : POSITIVE_WORDS) {
                if (content.contains(positive)) {
                    score++;
                    totalTerms++;
                }
            }

            for (String negative : NEGATIVE_WORDS) {
                if (content.contains(negative)) {
                    score--;
                    totalTerms++;
                }
            }
        }

        if (totalTerms == 0) {
            return 0.0;
        }

        double normalized = (double) score / totalTerms;
        return Math.max(-1.0, Math.min(1.0, normalized));
    }
}
