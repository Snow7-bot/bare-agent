package com.agent.llm;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("正在连接 DeepSeek...");

        // 创建客户端
        LlmClient client = new LlmClient();
        Scanner sc = new Scanner(System.in);
        System.out.println("连接成功");
        // 发送消息
        while (true) {
            String s=sc.next();
            String reply = client.chat(s);
            if(s.equals("拜拜")) {
                System.out.println("baibai");
                break;
            }
            // 打印回复
            System.out.println("DeepSeek 回复：");
            System.out.println(reply);
        }
    }
}