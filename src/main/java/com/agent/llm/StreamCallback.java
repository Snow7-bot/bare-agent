package com.agent.llm;

public interface StreamCallback {
    /**
     * 每收到一个 token 就调用一次
     */
    void onToken(String token);

    /**
     * 流式输出完成时调用
     */
    void onComplete(String fullContent);

    /**
     * 出错时调用
     */
    void onError(Throwable error);
}