package com.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ToolExecutor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 入口：根据工具名路由到具体实现
     */
    public String execute(String toolName, String arguments) {
        try {
            Map<String, Object> args = objectMapper.readValue(arguments, Map.class);
            return switch (toolName) {
                case "get_current_time" -> getCurrentTime();
                case "calculate" -> calculate(args);
                case "read_file" -> readFile(args);
                default -> "{\"error\": \"未知工具: " + toolName
                        + "，请检查可用工具列表后重试\"}";
            };
        } catch (Exception e) {
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    // ====== 工具1：获取当前时间 ======
    private String getCurrentTime() {
        String now = Instant.now()
                .atZone(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "{\"time\": \"" + now + "\"}";
    }

    // ====== 工具2：计算器 ======
    private String calculate(Map<String, Object> args) {
        // 参数校验：缺少必填字段
        if (!args.containsKey("expression") || args.get("expression") == null) {
            return "{\"error\": \"缺少必填参数: expression，请提供数学表达式\"}";
        }

        String expression = (String) args.get("expression");
        String expr = expression.replaceAll("\\s+", "");

        // 安全检查
        if (!expr.matches("[0-9+\\-*/().]+")) {
            return "{\"error\": \"表达式包含非法字符，只允许数字和 + - * / ( )\"}";
        }

        // 除零检查
        if (expr.contains("/0") && !expr.contains("/0.")) {
            return "{\"error\": \"除零错误：表达式中包含除以零的操作\"}";
        }

        try {
            double result = eval(expr);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "{\"error\": \"计算结果无效（NaN或无穷大），请检查表达式\"}";
            }
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "{\"result\": " + (long) result + "}";
            }
            return "{\"result\": " + result + "}";
        } catch (Exception e) {
            return "{\"error\": \"计算失败: " + e.getMessage() + "，请检查表达式格式\"}";
        }
    }

    // ====== 工具3：读文件 ======
    private String readFile(Map<String, Object> args) {
        // 参数校验
        if (!args.containsKey("path") || args.get("path") == null) {
            return "{\"error\": \"缺少必填参数: path，请提供文件路径\"}";
        }

        String path = (String) args.get("path");
        if (path.isEmpty()) {
            return "{\"error\": \"文件路径不能为空\"}";
        }

        Path filePath = Path.of(path);
        if (Files.isDirectory(filePath)) {
            return "{\"error\": \"路径是一个目录，不是文件: " + path + "\"}";
        }
        if (!Files.exists(filePath)) {
            return "{\"error\": \"文件不存在: " + path + "\"}";
        }

        try {
            String content = Files.readString(filePath);
            return "{\"content\": \""
                    + content.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t")
                    + "\"}";
        } catch (IOException e) {
            return "{\"error\": \"文件读取失败: " + e.getMessage() + "\"}";
        }
    }

    // ====== 递归下降计算器（不变） ======
    private double eval(String expr) {
        return new Object() {
            int pos = -1;
            char ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : (char) -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("多余字符: " + ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, pos));
                } else {
                    throw new RuntimeException("无法识别的字符: " + ch);
                }
                return x;
            }
        }.parse();
    }
}