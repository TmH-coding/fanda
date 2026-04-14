package com.fanda.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 配置
 * 使用通义千问 DashScope 作为底层模型
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是"饭搭"App的智能助手小饭，专注于帮助都市打工人做饮食决策。
                        你的风格亲切、简洁，建议具体实用。回答用中文，不超过300字。
                        """)
                .build();
    }
}
