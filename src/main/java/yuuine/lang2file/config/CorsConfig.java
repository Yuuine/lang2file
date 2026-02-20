package yuuine.lang2file.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://127.0.0.1:8000",
                        "http://localhost:8000",
                        "http://192.168.101.12:8000",
                        "http://192.168.154.1:8000",
                        "http://192.168.136.1:8000"
                )  // 允许的前端地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(false)  // 不允许携带凭证
                .maxAge(3600);  // 预检请求缓存时间
    }
}