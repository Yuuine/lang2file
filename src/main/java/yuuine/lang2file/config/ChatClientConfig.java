package yuuine.lang2file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ChatClientConfig {

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
}