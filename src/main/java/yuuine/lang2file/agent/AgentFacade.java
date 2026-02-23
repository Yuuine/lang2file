package yuuine.lang2file.agent;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import yuuine.lang2file.agent.dto.ToolMeta;
import yuuine.lang2file.agent.dto.ToolRegistry;
import yuuine.lang2file.agent.service.ChatClientFactory;
import yuuine.lang2file.agent.service.DynamicToolProviderFactory;
import yuuine.lang2file.agent.service.TaskChatRouterService;
import yuuine.lang2file.agent.service.ToolRouterService;
import yuuine.lang2file.util.IdUtil;

import java.util.List;

/**
 * 业务外观类，负责编排智能体对话的核心流程。
 * <p>
 * 根据用户输入判断是否为任务型对话，分别走不同的处理路径：
 * <ul>
 *   <li>非任务型对话：使用不带工具的普通聊天客户端直接响应。</li>
 *   <li>任务型对话：动态选取相关工具，构造带工具的客户端进行响应。</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AgentFacade {

    private final ToolRouterService router;
    private final ToolRegistry registry;
    private final DynamicToolProviderFactory factory;
    private final ChatClientFactory clientFactory;
    private final TaskChatRouterService taskRouter;

    /**
     * 处理用户输入的聊天消息，返回智能体的响应。
     *
     * @param userInput 用户输入的文本
     * @return 聊天响应对象 {@link ChatResponse}
     */
    public ChatResponse chat(String userInput) {
        // 判断是否为任务型对话，分流处理
        if (!taskRouter.isTask(userInput)) {
            return handleNonTaskChat(userInput);
        } else {
            return handleTaskChat(userInput);
        }
    }

    /**
     * 处理非任务型对话：使用不带工具的普通聊天客户端。
     *
     * @param userInput 用户输入
     * @return 聊天响应
     */
    private ChatResponse handleNonTaskChat(String userInput) {
        // 生成临时会话ID，创建无工具的客户端
        ChatClient client = clientFactory.createClientWithoutTools(IdUtil.generateSessionId());
        return client.prompt()
                .user(userInput)
                .call()
                .chatResponse();
    }

    /**
     * 处理任务型对话：动态选取工具，构造带工具的客户端进行响应。
     *
     * @param userInput 用户输入
     * @return 聊天响应
     */
    private ChatResponse handleTaskChat(String userInput) {
        // 1. 根据用户输入选取匹配的工具名称列表
        List<String> toolNames = router.selectTools(userInput);

        // 2. 根据工具名称获取对应的工具元信息
        List<ToolMeta> metas = registry.getByNames(toolNames);

        // 3. 动态构建工具回调提供者
        ToolCallbackProvider provider = factory.build(metas);

        // 4. 创建绑定了工具的聊天客户端
        ChatClient client = clientFactory.createClient(provider);

        // 5. 执行提示并返回响应
        return client.prompt()
                .user(userInput)
                .call()
                .chatResponse();
    }
}