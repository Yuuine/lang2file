package yuuine.lang2file.agent.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import yuuine.lang2file.agent.AgentFacade;
import yuuine.lang2file.agent.AgentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final AgentFacade agentFacade;

    /**
     * 同步聊天方法 - 返回完整响应
     *
     * @param userInput 用户输入
     * @return AI助手的完整回复
     */
    @Override
    public ChatResponse chat(String userInput) {
        try {
            log.debug("处理同步聊天请求: {}", userInput);

            ChatResponse chatResponse = agentFacade.chat(userInput);

            if (chatResponse != null) {
                log.debug(String.valueOf(chatResponse.getResult().getOutput()));
                log.info("获取对话成功，详情见debug日志");
            }

            return chatResponse;

        } catch (Exception e) {
            log.error("处理同步聊天请求失败: {}", userInput, e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * @param userInput 用户输入
     * @return 暂时不返回内容
     */
    @Override
    public Flux<String> chatStream(String userInput) {
        return null;
    }
}