package yuuine.lang2file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId("default-session")
                                .build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {

        InMemoryChatMemoryRepository repository = new InMemoryChatMemoryRepository();

        // 20条对话，10轮对话
        final int MAX_MESSAGES = 20;

        return new ChatMemory() {
            @Override
            public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
                List<Message> existingMessages = new ArrayList<>(get(conversationId));
                existingMessages.addAll(messages);

                while (existingMessages.size() > MAX_MESSAGES) {
                    existingMessages.removeFirst();
                }

                repository.saveAll(conversationId, existingMessages);
            }

            @Override
            public @NonNull List<Message> get(@NonNull String conversationId) {
                return repository.findByConversationId(conversationId);
            }

            @Override
            public void clear(@NonNull String conversationId) {
                repository.deleteByConversationId(conversationId);
            }
        };
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