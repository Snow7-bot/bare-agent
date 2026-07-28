package com.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StructuredExtractor {

    private final String apiKey;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StructuredExtractor() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream("application.properties"));

        this.apiKey = props.getProperty("deepseek.api.key");
        this.apiUrl = props.getProperty("deepseek.api.url");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public <T> T extract(String text, String schemaDescription,
                         Map<String, Object> jsonSchema,
                         Class<T> targetClass) throws IOException, InterruptedException {

        String systemPrompt = String.format(
                "你是一个数据提取专家。从用户提供的文本中提取信息，输出符合以下格式的 JSON。\n" +
                        "\n输出格式要求：\n%s\n\nJSON Schema:\n%s\n\n" +
                        "只输出 JSON，不要加任何解释或 markdown 标记。",
                schemaDescription,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema)
        );

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-v4-pro",
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0.1
        );

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        ChatResponse chatResponse = objectMapper.readValue(
                response.body(), ChatResponse.class);
        String jsonContent = chatResponse.getFirstContent();

        if (jsonContent.startsWith("```")) {
            jsonContent = jsonContent
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
        }

        return objectMapper.readValue(jsonContent, targetClass);
    }
}