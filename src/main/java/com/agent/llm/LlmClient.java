package com.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LlmClient {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LlmClient() throws IOException {
        // 1. 读取配置文件
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream("application.properties"));

        this.apiKey = props.getProperty("deepseek.api.key");
        this.apiUrl = props.getProperty("deepseek.api.url");
        this.model = props.getProperty("deepseek.model");

        // 2. 初始化 HTTP 客户端
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送消息给 LLM，返回回复文本
     */
    public String chat(String userMessage) throws IOException {
        // 1. 构造请求体 JSON
        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "user", "content", userMessage)
                        ),
                        "temperature", 0.7
                )
        );

        // 2. 构造 HTTP 请求
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody,
                        MediaType.get("application/json; charset=utf-8")))
                .build();

        // 3. 发送请求，拿到响应
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();

            // 4. 解析 JSON，提取回复内容
            JsonNode root = objectMapper.readTree(responseBody);
            return root.get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();
        }
    }
}