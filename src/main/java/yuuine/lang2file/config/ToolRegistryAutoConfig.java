package yuuine.lang2file.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yuuine.lang2file.agent.dto.ToolMeta;
import yuuine.lang2file.agent.dto.ToolRegistry;

import java.lang.reflect.Method;

@Configuration
@RequiredArgsConstructor
public class ToolRegistryAutoConfig {

    private final ApplicationContext context;

    @Bean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        context.getBeansOfType(Object.class).values().forEach(bean -> {
            for (Method m : bean.getClass().getDeclaredMethods()) {

                Tool tool = m.getAnnotation(Tool.class);
                if (tool != null) {

                    registry.register(new ToolMeta(
                            tool.name(),
                            tool.description(),
                            bean,
                            m
                    ));
                }
            }
        });

        return registry;
    }
}