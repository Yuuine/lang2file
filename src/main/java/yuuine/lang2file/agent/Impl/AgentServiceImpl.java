package yuuine.lang2file.agent.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import yuuine.lang2file.agent.AgentService;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;

    @Override
    public String process(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }
}
