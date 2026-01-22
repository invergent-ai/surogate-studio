package net.statemesh.config;

import lombok.RequiredArgsConstructor;
import net.statemesh.web.interceptor.PublicRateLimitInterceptor;
import net.statemesh.web.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfiguration implements WebMvcConfigurer {

    private final RateLimitInterceptor interceptor;
    private final PublicRateLimitInterceptor publicInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns("/api/**");

        registry.addInterceptor(publicInterceptor)
            .addPathPatterns("/public/**");
    }
}
