package yuuine.lang2file.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;
import yuuine.lang2file.agent.dto.ToolMeta;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicToolProviderFactory {

    public ToolCallbackProvider build(List<ToolMeta> metas) {

        log.debug("开始构建工具提供者: {}", metas);

        Set<Object> uniqueBeans = metas.stream()
                .map(ToolMeta::getBean)
                .collect(Collectors.toSet());

        log.debug("工具提供者构建完成: {}", uniqueBeans);

        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(uniqueBeans.toArray())
                .build();

        return provider;
    }

}