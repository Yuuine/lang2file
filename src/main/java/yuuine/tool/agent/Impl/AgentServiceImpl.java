package yuuine.tool.agent.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import yuuine.tool.agent.AgentService;

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
