package com.agent.llm;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class TokenCounter {

    private final Encoding encoding;
    private final ObjectMapper objectMapper;

    public TokenCounter() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 计算单条消息的 token 数
     */
    public int countTokens(Message message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return encoding.countTokens(json);
        } catch (Exception e) {
            return roughCount(message.getRole() + message.getContent());
        }
    }

    /**
     * 计算整个对话历史的 token 数
     */
    public int countTotal(List<Message> history) {
        int total = 0;
        for (Message msg : history) {
            total += countTokens(msg);
        }
        return total;
    }

    /**
     * 粗略估算（JTokkit 不可用时的降级方案）
     */
    public int roughCount(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) (chineseChars * 1.5 + otherChars * 0.3);
    }

    public Encoding getEncoding() {
        return encoding;
    }
}