package yuuine.lang2file.config;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ToolAutoConfig {

    @Bean
    public List<Object> toolBeans(ApplicationContext context) {
        // 扫描所有包含 @Tool 方法的 Bean
        return context.getBeansOfType(Object.class).values().stream()
                .filter(this::hasToolMethods)
                .toList();
    }

    private boolean hasToolMethods(Object bean) {
        return Arrays.stream(bean.getClass().getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(Tool.class));
    }
}