package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCall {
    private String id;
    private String type;

    @JsonProperty("function")
    private FunctionCall function;

    public ToolCall() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public FunctionCall getFunction() { return function; }
    public void setFunction(FunctionCall function) { this.function = function; }

    // ====== 内部类 ======
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionCall {
        private String name;
        private String arguments;

        public FunctionCall() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
}