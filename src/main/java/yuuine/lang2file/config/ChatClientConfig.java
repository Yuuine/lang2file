package yuuine.lang2file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {

        ChatClient client = builder
                .defaultTools()
                .build();
        
        log.info("ChatClient 创建成功");

        return client;
    }
}
