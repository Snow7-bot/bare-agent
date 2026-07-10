package com.agent.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PromptTemplate {

    private final String template;

    // 构造方法：加载模板文件
    public PromptTemplate(String filePath) throws IOException {
        // 从 resources 目录读取文件
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(filePath)) {
            if (is == null) {
                throw new IOException("模板文件不存在: " + filePath);
            }
            this.template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 替换占位符，生成最终 Prompt
     * 例如：模板里有 {name}，传入 Map.of("name", "张三")
     */
    public String render(Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            // 把 {key} 替换成 value
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // 获取原始模板（调试用）
    public String getTemplate() {
        return template;
    }
}