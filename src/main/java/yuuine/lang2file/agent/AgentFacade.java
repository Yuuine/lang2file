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
 * 业务编排层
 */
@Service
@RequiredArgsConstructor
public class AgentFacade {

    private final ToolRouterService router;
    private final ToolRegistry registry;
    private final DynamicToolProviderFactory factory;
    private final ChatClientFactory clientFactory;
    private final TaskChatRouterService taskRouter;

    public ChatResponse chat(String userInput) {

        // 对话路由判断（任务型对话/非任务型对话）
        if (!taskRouter.isTask(userInput)) {
            // 非任务型对话

            ChatClient client = clientFactory.createClientWithoutTools(IdUtil.generateSessionId());
            ChatResponse chatResponse = client.prompt()
                    .user(userInput)
                    .call()
                    .chatResponse();

            return chatResponse;
        }

        // 1. 获取全部 tool，构建一个 List 列表（第一轮响应）
        List<String> toolNames = router.selectTools(userInput);

        // 2. 构建 tool 的元信息列表
        List<ToolMeta> metas = registry.getByNames(toolNames);

        // 3. 动态构建 ToolProvider
        ToolCallbackProvider provider = factory.build(metas);

        // 4. client 工厂构建专用 ChatClient
        ChatClient client = clientFactory.createClient(provider);

        // 5. 第二轮响应
        ChatResponse chatResponse = client.prompt()
                .user(userInput)
                .call()
                .chatResponse();

        return chatResponse;
    }
}