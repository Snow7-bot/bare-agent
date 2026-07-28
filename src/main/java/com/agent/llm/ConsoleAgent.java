package com.agent.llm;

import com.agent.llm.model.*;

import java.io.IOException;
import java.util.*;

public class ConsoleAgent {
    private static final String LOG_FILE = "src/main/resources/logs/usage.log";
    private static final int MAX_TOKENS = 128000;
    private static final double THRESHOLD = 0.8;
    private static final int MAX_ITERATIONS = 10;

    private final LlmClient client;
    private final ToolRegistry registry;
    private final ToolExecutor executor;
    private final TokenCounter tokenCounter;
    private final List<Message> history;
    private int roundCount;

    public ConsoleAgent() throws IOException {
        this.client = new LlmClient();
        this.registry = new ToolRegistry();
        this.executor = new ToolExecutor();
        this.tokenCounter = new TokenCounter();
        this.history = new ArrayList<>();
        this.roundCount = 0;

        registerTools();
        loadSystemPrompt();
    }

    private void registerTools() {
        registry.register(new ToolDefinition("get_current_time", "获取当前系统时间（北京时间）",
                Map.of("type", "object", "properties", Map.of())));
        registry.register(new ToolDefinition("calculate", "执行数学表达式计算",
                Map.of("type", "object", "properties", Map.of("expression",
                                Map.of("type", "string", "description", "数学表达式")),
                        "required", List.of("expression"))));
        registry.register(new ToolDefinition("read_file", "读取文件内容",
                Map.of("type", "object", "properties", Map.of("path",
                                Map.of("type", "string", "description", "文件路径")),
                        "required", List.of("path"))));
        registry.register(new ToolDefinition("web_search", "搜索互联网信息（模拟）",
                Map.of("type", "object", "properties", Map.of("query",
                                Map.of("type", "string", "description", "搜索关键词")),
                        "required", List.of("query"))));
        registry.register(new ToolDefinition("save_to_file", "保存内容到文件",
                Map.of("type", "object", "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "content", Map.of("type", "string", "description", "要保存的内容")),
                        "required", List.of("path", "content"))));
        registry.register(new ToolDefinition("list_files", "列出目录下的文件",
                Map.of("type", "object", "properties", Map.of("path",
                                Map.of("type", "string", "description", "目录路径")),
                        "required", List.of())));
    }

    private void loadSystemPrompt() throws IOException {
        PromptTemplate agentPrompt = new PromptTemplate("prompts/agent_system.txt");
        history.add(new Message("system", agentPrompt.getTemplate()));
    }

    public void start() throws Exception {
        System.out.println("========================================");
        System.out.println("  ConsoleAgent v1.0 — Java AI Agent");
        System.out.println("  已注册 " + registry.size() + " 个工具 | Token上限: " + MAX_TOKENS);
        System.out.println("  试试问：\"现在几点了？\" 或 \"搜索Java Agent并保存\"");
        System.out.println("========================================\n");

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("你: ");
            String input = sc.nextLine();
            if (input.equals("拜拜")) { System.out.println("再见！"); break; }

            history.add(new Message("user", input));

            ChatResponse response = client.chatWithTools(history, registry.getAllDefinitions());

            if (response.hasToolCalls()) {
                handleToolCalls(response);
            } else {
                String reply = response.getFirstContent();
                history.add(new Message("assistant", reply));
                System.out.println("DeepSeek: " + reply);
            }
            roundCount++;
            System.out.println("  [第 " + roundCount + " 轮 | Token: "
                    + tokenCounter.countTotal(history) + "/" + MAX_TOKENS + "]\n");
        }
        sc.close();
    }

    private void handleToolCalls(ChatResponse response) throws Exception {
        List<ToolCall> toolCalls = response.getToolCalls();
        history.add(response.getChoices().get(0).getMessage());

        for (ToolCall tc : toolCalls) {
            String name = tc.getFunction().getName();
            String args = tc.getFunction().getArguments();
            System.out.println("🔧 执行: " + name + "(" + args + ")");
            String result = executor.execute(name, args);
            System.out.println("   结果: " + result);
            history.add(new Message("tool", result, tc.getId()));
        }

        // 继续循环直到 LLM 不再需要工具
        while (true) {
            ChatResponse tr = client.chatWithTools(history, registry.getAllDefinitions());
            if (tr.hasToolCalls()) {
                history.add(tr.getChoices().get(0).getMessage());
                for (ToolCall tc : tr.getToolCalls()) {
                    String name = tc.getFunction().getName();
                    String args = tc.getFunction().getArguments();
                    System.out.println("🔧 执行: " + name + "(" + args + ")");
                    String result = executor.execute(name, args);
                    System.out.println("   结果: " + result);
                    history.add(new Message("tool", result, tc.getId()));
                }
            } else {
                System.out.print("DeepSeek: ");
                try {
                    client.chatStream(history, registry.getAllDefinitions(), new StreamCallback() {
                        @Override public void onToken(String t) { System.out.print(t); }
                        @Override public void onComplete(String full) {
                            System.out.println();
                            history.add(new Message("assistant", full));
                        }
                        @Override public void onError(Throwable e) {
                            System.err.println("\n流式错误: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("流式失败: " + e.getMessage());
                }
                break;
            }
        }
    }

    // ====== Day 11：结构化提取（直接集成） ======
    public Person extractPerson(String text) throws Exception {
        StructuredExtractor se = new StructuredExtractor();
        return se.extract(text, "提取人物信息",
                Map.of("type", "object", "properties", Map.of(
                                "name", Map.of("type", "string"),
                                "age", Map.of("type", "integer"),
                                "phone", Map.of("type", "string"),
                                "email", Map.of("type", "string"),
                                "skills", Map.of("type", "string")),
                        "required", List.of("name")),
                Person.class);
    }
}