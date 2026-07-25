package com.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class LlmClient {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryPolicy retryPolicy;

    public LlmClient() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream("application.properties"));

        this.apiKey = props.getProperty("deepseek.api.key");
        this.apiUrl = props.getProperty("deepseek.api.url");
        this.model = props.getProperty("deepseek.model");

        // 设置超时：连接10秒，读取60秒
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.retryPolicy = new RetryPolicy(3, "src/main/resources/logs/retry.log");
    }

    /**
     * 带工具列表的对话（含重试逻辑）
     */
    public ChatResponse chatWithTools(List<Message> messages, List<ToolDefinition> tools)
            throws LlmException, IOException {
        retryPolicy.reset();

        while (true) {
            try {
                return doChat(messages, tools);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("请求被中断: " + e.getMessage());
            } catch (IOException e) {
                String msg = e.getMessage();
                int statusCode = extractStatusCode(msg);

                // 401 — 不重试，Key 有问题
                if (statusCode == 401) {
                    throw new LlmException("API Key 无效，请检查 application.properties", 401);
                }

                // 429 / 503 — 可重试
                if (statusCode == 429 || statusCode == 503) {
                    if (retryPolicy.canRetry()) {
                        retryPolicy.waitAndLog(msg, statusCode);
                        continue;
                    }
                    throw new LlmException("重试耗尽: " + msg, statusCode, retryPolicy.getRetryCount());
                }

                // 其他错误
                throw new LlmException("LLM 调用失败: " + msg, statusCode);
            }
        }
    }


    /**
     * 真正发请求
     */
    private ChatResponse doChat(List<Message> messages, List<ToolDefinition> tools)
            throws IOException, InterruptedException, LlmException {

        ChatRequest request = new ChatRequest(model, messages);
        request.setTemperature(0.7);
        if (tools != null && !tools.isEmpty()) {
            request.setTools(tools);
        }

        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new IOException("HTTP " + statusCode + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ChatResponse.class);
    }
    /**
     * 流式对话：每收到一个 token 就回调 onToken，最后回调 onComplete
     */
    public void chatStream(List<Message> messages, List<ToolDefinition> tools,
                           StreamCallback callback) {

        retryPolicy.reset();

        while (true) {
            try {
                doChatStream(messages, tools, callback);
                return;
            } catch (IOException e) {
                String msg = e.getMessage();
                int statusCode = extractStatusCode(msg);

                if (statusCode == 401) {
                    callback.onError(new LlmException("API Key 无效", 401));
                    return;
                }

                if (statusCode == 429 || statusCode == 503) {
                    if (retryPolicy.canRetry()) {
                        try {
                            retryPolicy.waitAndLog(msg, statusCode);
                        } catch (LlmException ex) {
                            callback.onError(ex);
                            return;
                        }
                        continue;
                    }
                }

                callback.onError(new LlmException("流式调用失败: " + msg, statusCode));
                return;
            }
        }
    }

    /**
     * 真正执行流式请求
     */
    private void doChatStream(List<Message> messages, List<ToolDefinition> tools,
                              StreamCallback callback) throws IOException {

        ChatRequest request = new ChatRequest(model, messages);
        request.setTemperature(0.7);
        request.setStream(true);          // ★ 开启流式
        if (tools != null && !tools.isEmpty()) {
            request.setTools(tools);
        }

        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<java.io.InputStream> response = null;
        try {
            response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int statusCode = response.statusCode();
        if (statusCode != 200) {
            String errorBody = new String(response.body().readAllBytes());
            throw new IOException("HTTP " + statusCode + ": " + errorBody);
        }

        // 逐行读取 SSE 事件流
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body()));
        String line;
        StringBuilder fullContent = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                try {
                    ChatResponse chunk = objectMapper.readValue(data, ChatResponse.class);
                    if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                        ChatResponse.Choice choice = chunk.getChoices().get(0);
                        Message delta = choice.getDelta();
                        if (delta == null) {
                            delta = choice.getMessage();
                        }
                        if (delta != null && delta.getContent() != null) {
                            String token = delta.getContent();
                            fullContent.append(token);
                            callback.onToken(token);
                        }
                    }
                } catch (Exception e) {
                    // 个别 chunk 解析失败不影响整体
                    System.err.println("SSE 解析失败: " + e.getMessage());
                }
            }
        }

        callback.onComplete(fullContent.toString());
    }

    /**
     * 从异常消息中提取 HTTP 状态码
     */
    private int extractStatusCode(String msg) {
        if (msg == null) return 0;
        if (msg.contains("401")) return 401;
        if (msg.contains("429")) return 429;
        if (msg.contains("503")) return 503;
        return 0;
    }
}