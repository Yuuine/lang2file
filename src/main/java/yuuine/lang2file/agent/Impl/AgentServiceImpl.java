package yuuine.lang2file.agent.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import yuuine.lang2file.agent.AgentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;

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

            ChatResponse chatResponse = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .chatResponse();


            if (chatResponse != null) {
                log.debug(String.valueOf(chatResponse.getResult().getOutput()));
            }
            if (chatResponse != null) {
                log.info(chatResponse.getResult().getOutput().getText());
            }

            return chatResponse;


        } catch (Exception e) {
            log.error("处理同步聊天请求失败: {}", userInput, e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式聊天方法 - 返回响应流
     *
     * @param userInput 用户输入
     * @return 响应内容的流
     */
    @Override
    public Flux<String> chatStream(String userInput) {
        try {
            log.debug("处理流式聊天请求: {}", userInput);

            return chatClient.prompt()
                    .user(userInput)
                    .stream()
                    .content()
                    .doOnNext(chunk -> log.trace("流式响应块: {}", chunk))
                    .doOnComplete(() -> log.debug("流式响应完成"))
                    .doOnError(error -> log.error("流式响应错误", error));

        } catch (Exception e) {
            log.error("处理流式聊天请求失败: {}", userInput, e);
            return Flux.error(new RuntimeException("AI流式服务调用失败: " + e.getMessage(), e));
        }
    }
}
