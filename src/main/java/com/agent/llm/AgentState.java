package com.agent.llm;

public enum AgentState {
    THINKING,   // 正在思考，准备决定下一步
    ACTING,     // LLM 请求了工具，正在执行
    OBSERVING,  // 工具执行完毕，观察结果
    FINISHED    // 对话结束，已给出最终回答
}