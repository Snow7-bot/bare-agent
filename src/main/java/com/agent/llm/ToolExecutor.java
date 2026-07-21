package com.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

public class ToolExecutor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据工具名和参数执行对应工具，返回结果字符串
     */
    public String execute(String toolName, String arguments) {
        try {
            Map<String, Object> args = objectMapper.readValue(arguments, Map.class);
            return switch (toolName) {
                case "get_current_time" -> getCurrentTime();
                case "calculate" -> calculate((String) args.get("expression"));
                case "read_file" -> readFile((String) args.get("path"));
                default -> "{\"error\": \"未知工具: " + toolName + "\"}";
            };
        } catch (Exception e) {
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    // ====== 3 个工具的真实实现 ======

    private String getCurrentTime() {
        String now = Instant.now()
                .atZone(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "{\"time\": \"" + now + "\"}";
    }

    private String calculate(String expression) {
        try {
            // 去掉所有空格
            String expr = expression.replaceAll("\\s+", "");
            // 只允许数字、运算符、括号、小数点
            if (!expr.matches("[0-9+\\-*/().]+")) {
                return "{\"error\": \"表达式包含非法字符\"}";
            }
            double result = eval(expr);
            // 整数就输出整数格式
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "{\"result\": " + (long) result + "}";
            }
            return "{\"result\": " + result + "}";
        } catch (Exception e) {
            return "{\"error\": \"计算失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 递归下降解析四则运算
     */
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

    private String readFile(String path) {
        try {
            String content = Files.readString(Path.of(path));
            return "{\"content\": \"" + content.replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
        } catch (IOException e) {
            return "{\"error\": \"文件读取失败: " + e.getMessage() + "\"}";
        }
    }
}