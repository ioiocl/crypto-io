package cl.ioio.finbot.aidecision.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class OpenAiDecisionClient {

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiDecisionClient(
            @ConfigProperty(name = "openai.api.key") String apiKey,
            @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o-mini") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> getDecisionJson(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY is not configured. Using fallback heuristic decisions.");
            return Optional.empty();
        }

        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(12000);
            connection.setDoOutput(true);

            String payload = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0.2)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "system")
                                    .put("content", "You are a trading analyst. Return only valid JSON with keys: signal, confidence, reasoning."))
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)))
                    .toString();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("OpenAI returned HTTP status {}", status);
                return Optional.empty();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonNode root = objectMapper.readTree(response.toString());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                return Optional.empty();
            }

            String content = contentNode.asText().trim();
            JsonNode parsed = objectMapper.readTree(content);
            return Optional.of(parsed);
        } catch (Exception e) {
            log.error("Error calling OpenAI", e);
            return Optional.empty();
        }
    }
}
