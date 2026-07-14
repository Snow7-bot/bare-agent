package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String role;
    private String content;
    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(String role, String content, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
    }

    // ====== Getter / Setter ======
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}