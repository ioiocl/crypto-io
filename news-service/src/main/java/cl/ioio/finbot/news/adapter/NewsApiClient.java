package cl.ioio.finbot.news.adapter;

import cl.ioio.finbot.news.model.NewsArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Slf4j
public class NewsApiClient {

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public NewsApiClient(@ConfigProperty(name = "news.api.key") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    public List<NewsArticle> fetchBySymbol(String symbol, int maxHeadlines) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("NEWS_API_KEY not configured. Skipping news fetch for {}", symbol);
            return List.of();
        }

        try {
            String query = URLEncoder.encode(symbol + " crypto", StandardCharsets.UTF_8);
            String endpoint = "https://newsdata.io/api/1/crypto?apikey=" +
                    URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                    "&q=" + query +
                    "&language=en&size=" + maxHeadlines;

            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("NewsData returned status {} for symbol {}", status, symbol);
                return List.of();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonNode root = objectMapper.readTree(response.toString());
            JsonNode articlesNode = root.path("results");
            if (!articlesNode.isArray()) {
                return List.of();
            }

            List<NewsArticle> articles = new ArrayList<>();
            for (JsonNode node : articlesNode) {
                articles.add(NewsArticle.builder()
                        .title(node.path("title").asText(""))
                        .description(node.path("description").asText(node.path("content").asText("")))
                        .build());
            }

            return articles;
        } catch (Exception e) {
            log.error("Error fetching NewsData for symbol {}", symbol, e);
            return List.of();
        }
    }
}
