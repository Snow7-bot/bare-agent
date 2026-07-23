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

        // 创建执行器和 ReAct 循环
        ToolExecutor executor = new ToolExecutor();
        ReActLoop reActLoop = new ReActLoop(client, registry, executor, 10);

        System.out.println("连接成功！已注册 " + registry.size() + " 个工具。");
        System.out.println("试试问：\"现在几点了？\" 或 \"先查当前时间，然后告诉我 2026 年元旦还有多少天\"\n");

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

            // 使用 ReAct 循环处理
            String finalReply = reActLoop.run(history);
            history.add(new Message("assistant", finalReply));

            System.out.println("DeepSeek: " + finalReply);

            // 统计
            roundCount++;
            System.out.println("  [ReAct 循环 " + reActLoop.getIterationCount()
                    + " 轮，状态: " + reActLoop.getState() + "]");
        }

        // 打印汇总
        int total = totalPromptTokens + totalCompletionTokens;
        System.out.println("\n========== 会话汇总 ==========");
        System.out.println("总轮次: " + roundCount);
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