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

    public static void main(String[] args) throws IOException, InterruptedException, LlmException {
        System.out.println("正在连接 DeepSeek...");
        LlmClient client = new LlmClient();
        Scanner sc = new Scanner(System.in);

        // ---- 创建工具箱并注册工具 ----
        ToolRegistry registry = new ToolRegistry();
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
        registry.register(new ToolDefinition(
                "web_search",
                "搜索互联网信息（模拟），返回相关结果",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "搜索关键词"
                                )
                        ),
                        "required", List.of("query")
                )
        ));
        registry.register(new ToolDefinition(
                "save_to_file",
                "将内容保存到指定文件",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "文件路径"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "要保存的内容"
                                )
                        ),
                        "required", List.of("path", "content")
                )
        ));
        registry.register(new ToolDefinition(
                "list_files",
                "列出指定目录下的文件",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "目录路径，默认为当前目录"
                                )
                        ),
                        "required", List.of()
                )
        ));

        // 创建执行器和 ReAct 循环
        ToolExecutor executor = new ToolExecutor();
        ReActLoop reActLoop = new ReActLoop(client, registry, executor, 10, 128000, 0.8);
        System.out.println("连接成功！已注册 " + registry.size() + " 个工具。");
        System.out.println("试试问：\"现在几点了？\" 或 \"帮我在网上搜索 Java Agent 的最新信息，然后保存到 agent-news.txt\"\n");

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

            if (response.hasToolCalls()) {
                List<ToolCall> toolCalls = response.getToolCalls();
                Message assistantMsg = response.getChoices().get(0).getMessage();
                history.add(assistantMsg);

                for (ToolCall tc : toolCalls) {
                    String toolName = tc.getFunction().getName();
                    String arguments = tc.getFunction().getArguments();
                    System.out.println("🔧 执行: " + toolName + "(" + arguments + ")");
                    String result = executor.execute(toolName, arguments);
                    System.out.println("   结果: " + result);
                    history.add(new Message("tool", result, tc.getId()));
                }

                while (true) {
                    ChatResponse toolResponse = client.chatWithTools(history, registry.getAllDefinitions());
                    if (toolResponse.hasToolCalls()) {
                        List<ToolCall> moreToolCalls = toolResponse.getToolCalls();
                        Message moreAssistantMsg = toolResponse.getChoices().get(0).getMessage();
                        history.add(moreAssistantMsg);

                        for (ToolCall tc : moreToolCalls) {
                            String toolName = tc.getFunction().getName();
                            String arguments = tc.getFunction().getArguments();
                            System.out.println("🔧 执行: " + toolName + "(" + arguments + ")");
                            String result = executor.execute(toolName, arguments);
                            System.out.println("   结果: " + result);
                            history.add(new Message("tool", result, tc.getId()));
                        }
                    } else {
                        System.out.print("DeepSeek: ");
                        try {
                            client.chatStream(history, registry.getAllDefinitions(), new StreamCallback() {
                                @Override
                                public void onToken(String token) {
                                    System.out.print(token);
                                }
                                @Override
                                public void onComplete(String fullContent) {
                                    System.out.println();
                                    history.add(new Message("assistant", fullContent));
                                }
                                @Override
                                public void onError(Throwable error) {
                                    System.err.println("\n流式错误: " + error.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("流式调用失败: " + e.getMessage());
                        }
                        break;
                    }
                }
            } else {
                String reply = response.getFirstContent();
                history.add(new Message("assistant", reply));
                System.out.println("DeepSeek: " + reply);
            }

            roundCount++;
            System.out.println("  [第 " + roundCount + " 轮]");
        }

        int total = totalPromptTokens + totalCompletionTokens;
        System.out.println("\n========== 会话汇总 ==========");
        System.out.println("总轮次: " + roundCount);
        System.out.println("日志已保存到: " + LOG_FILE);

        sc.close();
    }

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