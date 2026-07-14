package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
    private String id;
    private List<Choice> choices;
    private Usage usage;

    public ChatResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    // 普通回复文本
    public String getFirstContent() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            if (msg != null) {
                return msg.getContent();
            }
        }
        return null;
    }

    // 判断是否有工具调用
    public boolean hasToolCalls() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            return msg != null && msg.hasToolCalls();
        }
        return false;
    }

    // 获取工具调用列表
    public List<ToolCall> getToolCalls() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            if (msg != null) {
                return msg.getToolCalls();
            }
        }
        return List.of();
    }

    // ====== 内部类 ======

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        private String finishReason;

        public Choice() {}

        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }

        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        public Usage() {}

        public int getPromptTokens() { return promptTokens; }
        public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

        public int getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

        public int getTotalTokens() {
            return promptTokens + completionTokens;
        }
    }
}