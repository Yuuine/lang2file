package yuuine.lang2file.config;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import yuuine.lang2file.agent.dto.ToolMeta;
import yuuine.lang2file.agent.dto.ToolRegistry;

import java.lang.reflect.Method;

/**
 * 工具注册自动配置类。
 * <p>
 * 扫描 Spring 容器中所有 Bean 的方法，将带有 {@link Tool} 注解的方法自动注册到 {@link ToolRegistry} 中。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class ToolRegistryAutoConfig {

    private final ApplicationContext context;

    /**
     * 创建并配置工具注册中心 Bean。
     * <p>
     * 遍历容器中所有 Bean，通过反射查找带有 {@link Tool} 注解的方法，
     * 将方法元信息包装为 {@link ToolMeta} 并注册到 {@link ToolRegistry}。
     * </p>
     *
     * @return 已填充所有工具元信息的注册中心实例
     */
    @Bean
    public ToolRegistry toolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // 获取容器中所有 Bean 并逐个扫描其方法上的 @Tool 注解
        context.getBeansOfType(Object.class)
                .values()
                .forEach(bean -> registerToolsFromBean(bean, registry));

        return registry;
    }

    /**
     * 扫描单个 Bean 的所有方法，将带有 {@link Tool} 注解的方法注册到注册中心。
     *
     * @param bean     待扫描的 Bean 实例
     * @param registry 工具注册中心
     */
    private void registerToolsFromBean(Object bean, ToolRegistry registry) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool != null) {
                ToolMeta meta = new ToolMeta(
                        tool.name(),
                        tool.description(),
                        bean,
                        method
                );
                registry.register(meta);
            }
        }
    }
}