package com.agent.llm;

import java.io.IOException;
import java.util.List;

public class ReActLoop {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final int maxIterations;

    private AgentState state;
    private int iterationCount;

    public ReActLoop(LlmClient llmClient, ToolRegistry toolRegistry,
                     ToolExecutor toolExecutor, int maxIterations) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.maxIterations = maxIterations;
    }

    /**
     * 执行 ReAct 循环：思考 → 行动 → 观察 → 直到完成
     */
    public String run(List<Message> history) throws IOException, InterruptedException ,LlmException {
        state = AgentState.THINKING;
        iterationCount = 0;

        while (state != AgentState.FINISHED && iterationCount < maxIterations) {
            iterationCount++;
            System.out.println("\n--- 第 " + iterationCount + " 轮，状态: " + state + " ---");
            // Token 超限保护：保留 system + 最近 20 条消息
            if (history.size() > 22) {
                Message systemMsg = history.get(0);
                List<Message> recent = history.subList(history.size() - 20, history.size());
                history.clear();
                history.add(systemMsg);
                history.addAll(recent);
                System.out.println("⚠️ 历史过长，已裁剪（保留 system + 最近 20 条）");
            }
            // 调用 LLM
            ChatResponse response = llmClient.chatWithTools(
                    history, toolRegistry.getAllDefinitions());

            if (response.hasToolCalls()) {
                // ====== ACTING：有工具请求 ======
                state = AgentState.ACTING;
                List<ToolCall> toolCalls = response.getToolCalls();

                // 写入 assistant 消息（含 tool_calls）
                Message assistantMsg = response.getChoices().get(0).getMessage();
                history.add(assistantMsg);

                // 执行每个工具
                for (ToolCall tc : toolCalls) {
                    String toolName = tc.getFunction().getName();
                    String arguments = tc.getFunction().getArguments();
                    System.out.println("🔧 执行: " + toolName + "(" + arguments + ")");

                    String result = toolExecutor.execute(toolName, arguments);
                    System.out.println("   结果: " + result);

                    // 写入 tool 消息
                    history.add(new Message("tool", result, tc.getId()));
                }

                // ====== OBSERVING：观察结果，进入下一轮 ======
                state = AgentState.OBSERVING;

            } else {
                // ====== FINISHED：LLM 给出最终回答 ======
                state = AgentState.FINISHED;
                return response.getFirstContent();
            }
        }

        // 达到最大循环次数
        return "⚠️ 达到最大循环次数 (" + maxIterations + ")，Agent 停止。";
    }

    public int getIterationCount() { return iterationCount; }
    public AgentState getState() { return state; }
}