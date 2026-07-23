package com.agent.llm;

public class LlmException extends Exception {

    private final int statusCode;
    private final int retryCount;

    public LlmException(String message) {
        super(message);
        this.statusCode = 0;
        this.retryCount = 0;
    }

    public LlmException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.retryCount = 0;
    }

    public LlmException(String message, int statusCode, int retryCount) {
        super(message);
        this.statusCode = statusCode;
        this.retryCount = retryCount;
    }

    public int getStatusCode() { return statusCode; }
    public int getRetryCount() { return retryCount; }

    @Override
    public String toString() {
        if (statusCode > 0) {
            return String.format("LlmException[%d]: %s (重试%d次)",
                    statusCode, getMessage(), retryCount);
        }
        return "LlmException: " + getMessage();
    }
}