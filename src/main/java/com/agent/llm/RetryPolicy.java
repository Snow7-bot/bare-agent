package com.agent.llm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RetryPolicy {

    private final int maxRetries;
    private final String logFile;
    private int retryCount;

    public RetryPolicy(int maxRetries, String logFile) {
        this.maxRetries = maxRetries;
        this.logFile = logFile;
        this.retryCount = 0;
    }

    /**
     * 判断是否还能继续重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * 获取本次重试等待时间（指数退避：1s → 2s → 4s → 8s）
     */
    public long getWaitSeconds() {
        return (long) Math.pow(2, retryCount);  // 第0次等1秒，第1次等2秒...
    }

    /**
     * 执行一次重试等待，并记录日志
     */
    public void waitAndLog(String reason, int statusCode) throws LlmException {
        retryCount++;
        long waitSeconds = getWaitSeconds();

        // 记录日志
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.printf("[%s] 重试 %d/%d | 状态码: %d | 等待: %ds | 原因: %s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    retryCount, maxRetries, statusCode, waitSeconds, reason);
        } catch (IOException e) {
            System.err.println("写入重试日志失败: " + e.getMessage());
        }

        System.out.println("⏳ 重试 " + retryCount + "/" + maxRetries
                + "，等待 " + waitSeconds + " 秒...");

        try {
            Thread.sleep(waitSeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("重试被中断", statusCode, retryCount);
        }

        if (!canRetry()) {
            throw new LlmException("达到最大重试次数(" + maxRetries + ")", statusCode, retryCount);
        }
    }

    public int getRetryCount() { return retryCount; }

    public void reset() { retryCount = 0; }
}