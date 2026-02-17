package yuuine.lang2file.agent.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import yuuine.lang2file.agent.AgentService;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;

    @Override
    public Flux<String> chatStream(String userInput) {

        return chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }

}
