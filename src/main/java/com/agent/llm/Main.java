package com.agent.llm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final String LOG_FILE = "src/main/resources/logs/usage.log";
    private static int totalPromptTokens = 0;
    private static int totalCompletionTokens = 0;
    private static int roundCount = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("正在连接 DeepSeek...");
        LlmClient client = new LlmClient();
        Scanner sc = new Scanner(System.in);

        // ---- Day 4: 创建工具箱并注册工具 ----
        ToolRegistry registry = new ToolRegistry();
        ToolExecutor executor = new ToolExecutor();
        registry.register(new ToolDefinition(
                "get_current_time",
                "获取当前系统时间（北京时间）",
                Map.of("type", "object", "properties", Map.of())
        ));
        registry.register(new ToolDefinition(
                "calculate",
                "执行数学表达式计算，支持 + - * / 和括号",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "expression", Map.of(
                                        "type", "string",
                                        "description", "数学表达式，如 \"123 * 456\" 或 \"(1+2)*3\""
                                )
                        ),
                        "required", List.of("expression")
                )
        ));
        registry.register(new ToolDefinition(
                "read_file",
                "读取指定路径的文件内容（仅限文本文件）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "文件的绝对或相对路径"
                                )
                        ),
                        "required", List.of("path")
                )
        ));
        System.out.println("连接成功！已注册 " + registry.size() + " 个工具。");
        System.out.println("试试问：\"现在几点了？\" 或 \"帮我算 123*456\"\n");

        // 初始化日志
        initLog();

        // 对话历史
        List<Message> history = new ArrayList<>();
        PromptTemplate agentPrompt = new PromptTemplate("prompts/agent_system.txt");
        String systemPrompt = agentPrompt.getTemplate();
        history.add(new Message("system", systemPrompt));

        // ====== 对话循环 ======
        while (true) {
            System.out.print("你: ");
            String userInput = sc.nextLine();

            if (userInput.equals("拜拜")) {
                System.out.println("再见！");
                break;
            }

            history.add(new Message("user", userInput));
            ChatResponse response = client.chatWithTools(history, registry.getAllDefinitions());

            // 判断：工具调用 还是 普通回复
            if (response.hasToolCalls()) {
                List<ToolCall> toolCalls = response.getToolCalls();

                // 1. 把 assistant 消息（含 tool_calls）写入历史
                Message assistantMsg = response.getChoices().get(0).getMessage();
                history.add(assistantMsg);

                // 2. 执行每个工具，把结果写入历史
                for (ToolCall tc : toolCalls) {
                    String toolName = tc.getFunction().getName();
                    String arguments = tc.getFunction().getArguments();
                    System.out.println("\n🔧 执行工具: " + toolName);
                    System.out.println("   参数: " + arguments);

                    // ★ 真正执行工具
                    String result = executor.execute(toolName, arguments);
                    System.out.println("   结果: " + result);

                    // 3. 封装成 tool 消息，写入历史
                    history.add(new Message("tool", result, tc.getId()));
                }

                // 4. 再次调用 LLM，让它基于工具结果生成最终回答
                System.out.println("\n🤖 LLM 基于工具结果生成回答...");
                ChatResponse finalResponse = client.chatWithTools(history, registry.getAllDefinitions());
                String finalReply = finalResponse.getFirstContent();
                history.add(new Message("assistant", finalReply));

                System.out.println("DeepSeek: " + finalReply);

                // 统计 token
                roundCount++;
                int prompt = 0, completion = 0;
                if (finalResponse.getUsage() != null) {
                    prompt = finalResponse.getUsage().getPromptTokens();
                    completion = finalResponse.getUsage().getCompletionTokens();
                }
                totalPromptTokens += prompt;
                totalCompletionTokens += completion;
                int totalTokens = finalResponse.getUsage() != null ? finalResponse.getUsage().getTotalTokens() : 0;
                System.out.println("  [输入: " + prompt + " | 输出: " + completion + " | 本轮: " + totalTokens + " tokens]");

                writeLog(userInput, prompt, completion);
                continue;
            }

            // 普通回复
            String reply = response.getFirstContent();
            history.add(new Message("assistant", reply));

            // 统计
            roundCount++;
            int prompt = 0;
            int completion = 0;
            if (response.getUsage() != null) {
                prompt = response.getUsage().getPromptTokens();
                completion = response.getUsage().getCompletionTokens();
            }
            totalPromptTokens += prompt;
            totalCompletionTokens += completion;

            System.out.println("DeepSeek: " + reply);
            int totalTokens = response.getUsage() != null
                    ? response.getUsage().getTotalTokens() : 0;
            System.out.println("  [输入: " + prompt + " | 输出: " + completion
                    + " | 本轮: " + totalTokens + " tokens]");

            writeLog(userInput, prompt, completion);
        }

        // 打印汇总
        int total = totalPromptTokens + totalCompletionTokens;
        System.out.println("\n========== 会话汇总 ==========");
        System.out.println("总轮次: " + roundCount);
        System.out.println("总输入: " + totalPromptTokens + " tokens");
        System.out.println("总输出: " + totalCompletionTokens + " tokens");
        System.out.println("总消耗: " + total + " tokens");
        System.out.println("日志已保存到: " + LOG_FILE);

        sc.close();
    }

    // ========== 日志方法 ==========

    private static void initLog() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println("========== 新会话 "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + " ==========");
        }
    }

    private static void writeLog(String userInput, int promptTokens, int completionTokens) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.printf("[%s] 用户: %s | 输入: %d | 输出: %d | 本轮: %d%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    userInput.length() > 30 ? userInput.substring(0, 30) + "..." : userInput,
                    promptTokens, completionTokens, promptTokens + completionTokens);
        }
    }
}