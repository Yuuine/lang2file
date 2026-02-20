package yuuine.lang2file.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;
import yuuine.lang2file.agent.dto.ToolMeta;
import yuuine.lang2file.agent.dto.ToolRegistry;

@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final ToolRegistry toolRegistry;

    // 无工具、无记忆
    public ChatClient createClientWithoutMemoryAndTools() {
        return ChatClient.builder(chatModel).build();
    }

    // 有记忆、有工具（默认全局工具）（使用默认会话ID）默认构造器
    public ChatClient createDefaultClient() {

        ToolCallbackProvider toolCallbackProvider = MethodToolCallbackProvider.builder()
                .toolObjects(toolRegistry.getAll().stream()
                        .map(ToolMeta::getBean).distinct().toArray())
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId("default-session")
                                .build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // 有记忆、有工具（自定义工具）
    public ChatClient createClient(ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // 有记忆、有工具（指定会话ID）
    public ChatClient createClientWithSession(String sessionId, ToolCallbackProvider toolCallbackProvider) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (sessionId != null) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId).build());
        }
        if (toolCallbackProvider != null) {
            builder.defaultToolCallbacks(toolCallbackProvider);
        }
        return builder.build();
    }

    // 仅工具、无记忆
    public ChatClient createClientWithoutMemory(ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // 允许完全自定义
    public ChatClient createCustomClient(ChatClient.Builder customBuilder) {
        return customBuilder.build();
    }
}