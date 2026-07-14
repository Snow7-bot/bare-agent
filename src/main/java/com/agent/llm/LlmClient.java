package com.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;

public class LlmClient {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LlmClient() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream("application.properties"));

        this.apiKey = props.getProperty("deepseek.api.key");
        this.apiUrl = props.getProperty("deepseek.api.url");
        this.model = props.getProperty("deepseek.model");

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 不带工具的简单对话
     */
    public ChatResponse chat(List<Message> messages) throws IOException, InterruptedException {
        return chatWithTools(messages, null);
    }

    /**
     * 带工具列表的对话 — Day 4 Function Calling
     */
    public ChatResponse chatWithTools(List<Message> messages, List<ToolDefinition> tools) throws IOException, InterruptedException {
        ChatRequest request = new ChatRequest(model, messages);
        request.setTemperature(0.7);
        request.setTools(tools);
        if (tools != null && !tools.isEmpty()) {
            request.setTools(tools);
        }

        // 2. 序列化为 JSON
        String requestBody = objectMapper.writeValueAsString(request);

        // 3. 构造 HTTP 请求
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 4. 发送请求
        HttpResponse<String> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofString());

        // 5. 反序列化为 ChatResponse 对象
        return objectMapper.readValue(response.body(), ChatResponse.class);
    }
}