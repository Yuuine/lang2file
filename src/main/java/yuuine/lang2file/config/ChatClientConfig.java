package yuuine.lang2file.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * 聊天客户端配置类，提供带有消息数量上限的 {@link ChatMemory} Bean。
 * <p>
 * 该内存实现基于 {@link InMemoryChatMemoryRepository}，并限制每个会话最多保留 20 条消息，
 * 自动移除最旧的消息以保持上限。
 * </p>
 */
@Configuration
public class ChatClientConfig {

    /**
     * 每个会话允许保留的最大消息数量。
     * 设置为 20 条，对应大约 10 轮对话（每轮包含用户和助手各一条消息）。
     */
    private static final int MAX_MESSAGES = 20;

    /**
     * 创建并配置一个带有消息数量限制的 {@link ChatMemory} Bean。
     *
     * @return 受限于 {@link #MAX_MESSAGES} 的聊天内存实现
     */
    @Bean
    public ChatMemory chatMemory() {
        InMemoryChatMemoryRepository repository = new InMemoryChatMemoryRepository();

        return new ChatMemory() {
            @Override
            public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
                // 获取当前会话的现有消息，并合并新消息
                List<Message> existingMessages = new ArrayList<>(get(conversationId));
                existingMessages.addAll(messages);

                // 如果超出最大限制，则从头部移除最旧的消息，直到满足数量上限
                while (existingMessages.size() > MAX_MESSAGES) {
                    existingMessages.removeFirst();
                }

                // 将裁剪后的消息列表保存回存储库
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
}