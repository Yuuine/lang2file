package yuuine.lang2file.agent;

import reactor.core.publisher.Flux;

/**
 * @author yuuine
 * 统一 I/O 接口
 */
public interface AgentService {


//    String chatTools(String userInput);

    /**
     * 处理用户输入，返回处理结果（流式）
     *
     * @param userInput 用户输入
     * @return 处理结果
     */
    Flux<String> chatStream(String userInput);

}
