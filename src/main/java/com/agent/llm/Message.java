package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role;
    private String content;
    private String toolCallId;

    // 无参构造（Jackson 反序列化需要）
    public Message() {}

    // 快捷构造：role + content
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // 完整构造：含 toolCallId
    public Message(String role, String content, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
    }

    // ========== Getter / Setter ==========
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
}