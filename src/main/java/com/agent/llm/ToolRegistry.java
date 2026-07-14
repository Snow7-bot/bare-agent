package com.agent.llm;

import java.util.*;

public class ToolRegistry {
    // 存所有工具定义
    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    /**
     * 注册一个工具
     */
    public void register(ToolDefinition tool) {
        tools.put(tool.getFunction().getName(), tool);
    }

    /**
     * 获取所有工具定义（发给 LLM 的 tools 数组）
     */
    public List<ToolDefinition> getAllDefinitions() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 根据工具名获取工具定义
     */
    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    /**
     * 已注册的工具数量
     */
    public int size() {
        return tools.size();
    }
}