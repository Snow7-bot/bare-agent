package com.agent.llm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReActLoop {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final TokenCounter tokenCounter;
    private final int maxIterations;
    private final int maxTokens;       // 模型上下文上限
    private final double threshold;    // 裁剪阈值（80%）

    private AgentState state;
    private int iterationCount;

    public ReActLoop(LlmClient llmClient, ToolRegistry toolRegistry,
                     ToolExecutor toolExecutor, int maxIterations,
                     int maxTokens, double threshold) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.tokenCounter = new TokenCounter();
        this.maxIterations = maxIterations;
        this.maxTokens = maxTokens;
        this.threshold = threshold;
    }

    // 兼容旧构造器（默认 128K tokens，80% 阈值）
    public ReActLoop(LlmClient llmClient, ToolRegistry toolRegistry,
                     ToolExecutor toolExecutor, int maxIterations) {
        this(llmClient, toolRegistry, toolExecutor, maxIterations, 128000, 0.8);
    }

    /**
     * 执行 ReAct 循环
     */
    public String run(List<Message> history) throws IOException, InterruptedException, LlmException {
        state = AgentState.THINKING;
        iterationCount = 0;

        while (state != AgentState.FINISHED && iterationCount < maxIterations) {
            iterationCount++;
            System.out.println("\n--- 第 " + iterationCount + " 轮，状态: " + state + " ---");

            // ====== Token 管理 ======
            int tokenCount = tokenCounter.countTotal(history);
            System.out.println("📊 当前 token: " + tokenCount + " / " + maxTokens
                    + " (" + (tokenCount * 100 / maxTokens) + "%)");

            if (tokenCount > maxTokens * threshold) {
                System.out.println("⚠️ 超过阈值(" + (threshold * 100) + "%)，裁剪历史...");
                history = trimHistory(history);
                System.out.println("📊 裁剪后 token: " + tokenCounter.countTotal(history));
            }

            // 调用 LLM
            ChatResponse response = llmClient.chatWithTools(
                    history, toolRegistry.getAllDefinitions());

            if (response.hasToolCalls()) {
                state = AgentState.ACTING;
                List<ToolCall> toolCalls = response.getToolCalls();

                Message assistantMsg = response.getChoices().get(0).getMessage();
                history.add(assistantMsg);

                for (ToolCall tc : toolCalls) {
                    String toolName = tc.getFunction().getName();
                    String arguments = tc.getFunction().getArguments();
                    System.out.println("🔧 执行: " + toolName + "(" + arguments + ")");

                    String result = toolExecutor.execute(toolName, arguments);
                    System.out.println("   结果: " + result);

                    history.add(new Message("tool", result, tc.getId()));
                }

                state = AgentState.OBSERVING;

            } else {
                state = AgentState.FINISHED;
                return response.getFirstContent();
            }
        }

        return "⚠️ 达到最大循环次数 (" + maxIterations + ")，Agent 停止。";
    }

    /**
     * 滑动窗口裁剪：保留 system + 最近 N 轮
     */
    private List<Message> trimHistory(List<Message> history) {
        List<Message> trimmed = new ArrayList<>();
        Message systemMsg = null;

        for (Message msg : history) {
            if ("system".equals(msg.getRole())) {
                systemMsg = msg;      // 找到 system prompt
            }
        }

        if (systemMsg != null) {
            trimmed.add(systemMsg);   // 始终保留 system prompt
        }

        // 保留最近的消息（从后往前找，留足空间）
        int start = Math.max(systemMsg != null ? 1 : 0, history.size() - 15);
        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            if (!"system".equals(msg.getRole())) {
                trimmed.add(msg);
            }
        }

        System.out.println("   裁剪: " + history.size() + " 条 → " + trimmed.size() + " 条");
        return trimmed;
    }

    // ====== Getter ======
    public int getIterationCount() { return iterationCount; }
    public AgentState getState() { return state; }
}