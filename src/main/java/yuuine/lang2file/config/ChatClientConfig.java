package yuuine.lang2file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {
        return builder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(List<Object> toolBeans) {
        // 自动收集所有包含 @Tool 方法的 Bean
        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(toolBeans.toArray())
                .build();

        log.info("自动注册 {} 个工具类，共 {} 个工具方法",
                toolBeans.size(),
                provider.getToolCallbacks().length);
        return provider;
    }
}