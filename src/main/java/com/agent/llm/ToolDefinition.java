package com.agent.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

    private String type = "function";

    @JsonProperty("function")
    private FunctionDef function;

    public ToolDefinition() {}

    public ToolDefinition(String name, String description, Map<String, Object> parameters) {
        this.function = new FunctionDef(name, description, parameters);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public FunctionDef getFunction() { return function; }
    public void setFunction(FunctionDef function) { this.function = function; }

    // ====== 内部类 ======
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDef {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public FunctionDef() {}

        public FunctionDef(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}