package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private List<ToolDefinition> tools;

    // 无参构造
    public ChatRequest() {}

    // 快捷构造：model + messages
    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
        this.temperature = 0.7;
    }

    // ========== Getter / Setter ==========
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public List<ToolDefinition> getTools() { return tools; }
    public void setTools(List<ToolDefinition> tools) { this.tools = tools; }
}