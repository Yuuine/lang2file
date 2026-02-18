package yuuine.lang2file.agent;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * @author yuuine
 * 统一 I/O 接口
 */
public interface AgentService {

    /**
     * 处理用户输入，返回处理结果（同步）
     *
     * @param userInput 用户输入
     * @return 处理结果
     */
    ChatResponse chat(String userInput);

    /**
     * 处理用户输入，返回处理结果（流式）
     *
     * @param userInput 用户输入
     * @return 处理结果流
     */
    Flux<String> chatStream(String userInput);

}
