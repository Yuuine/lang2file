package yuuine.lang2file.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import yuuine.lang2file.agent.dto.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolRouterService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ChatClientFactory chatClientFactory;
    private final ToolRegistry toolRegistry;

    public List<String> selectTools(String userInput) {

        String toolDesc = toolRegistry.getAll().stream()
                .map(t -> t.getName() + " : " + t.getDescription())
                .collect(Collectors.joining("\n"));

        log.debug("工具列表: {}", toolDesc);

        String prompt = """
                你是一个智能工具选择器。请根据用户输入，从工具列表中选择最相关的工具。
                
                工具列表（名称 : 描述）：
                %s
                
                用户输入：
                %s
                
                选择规则：
                - 只返回与用户输入相关的工具
                - 如果没有相关工具，返回空数组
                - 严格按照以下JSON格式返回
                
                返回格式示例：
                {"tools":["工具名称1","工具名称2"]}
                """.formatted(toolDesc, userInput);

        ChatClient chatClient = chatClientFactory.createClientWithoutMemoryAndTools();

        ChatResponse chatResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String result = null;
        if (chatResponse != null) {
            result = chatResponse.getResult().getOutput().getText();
        }

        if (!isValidJson(result)) {
            log.warn("无效的JSON: {}", result);
            return new ArrayList<>();
        }

        try {
            List<String> tools = convertJsonToList(result);

            List<String> distinctTools = tools.stream()
                    .distinct()
                    .collect(Collectors.toList());

            if (tools.size() != distinctTools.size()) {
                log.warn("AI返回了重复的工具名称，已自动去重: {} -> {}", tools, distinctTools);
            }

            log.debug("最终选择的工具: {}", distinctTools);
            return distinctTools;

        } catch (Exception e) {
            log.error("解析工具选择结果失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 验证字符串是否为有效JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null) return false;
        try {
            mapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 将JSON字符串转换为List
     */
    public static List<String> convertJsonToList(String json) throws Exception {
        Map<String, List<String>> map = mapper.readValue(json, new TypeReference<>() {});
        List<String> tools = map.get("tools");
        return tools != null ? tools : new ArrayList<>();
    }
}