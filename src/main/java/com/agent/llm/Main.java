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
    // 日志文件路径
    private static final String LOG_FILE = "src/main/resources/logs/usage.log";
    private static int totalPromptTokens = 0;
    private static int totalCompletionTokens = 0;
    private static int roundCount = 0;

    public static void main(String[] args) throws Exception {

        System.out.println("正在连接 DeepSeek...");
        LlmClient client = new LlmClient();
        Scanner sc = new Scanner(System.in);
        System.out.println("连接成功！");

        // 初始化日志文件
        initLog();

        // 对话历史
        List<Message> history = new ArrayList<>();
        history.add(new Message("system", "你是一个有帮助的助手，用中文回答。"));

        // 对话循环
        while (true) {
            System.out.print("你: ");
            String userInput = sc.nextLine();

            if (userInput.equals("拜拜")) {
                System.out.println("再见！");
                break;
            }

            history.add(new Message("user", userInput));
            ChatResponse response = client.chat(history);
            String reply = response.getFirstContent();
            history.add(new Message("assistant", reply));

            // 统计
            roundCount++;
            int prompt = response.getUsage().getPromptTokens();
            int completion = response.getUsage().getCompletionTokens();
            totalPromptTokens += prompt;
            totalCompletionTokens += completion;

            // 控制台输出
            System.out.println("DeepSeek: " + reply);
            System.out.println("  [输入: " + prompt + " | 输出: " + completion
                    + " | 本轮: " + response.getUsage().getTotalTokens() + " tokens]");

            // 写入日志
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

    // 初始化日志文件
    private static void initLog() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println("========== 新会话 "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + " ==========");
        }
    }

    // 写入一条日志
    private static void writeLog(String userInput, int promptTokens, int completionTokens) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.printf("[%s] 用户: %s | 输入: %d | 输出: %d | 本轮: %d%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    userInput.length() > 30 ? userInput.substring(0, 30) + "..." : userInput,
                    promptTokens,
                    completionTokens,
                    promptTokens + completionTokens);
        }
    }
}