package com.shujichen.rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 配置异步请求处理，支持SSE流式响应
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 配置异步任务执行器，用于处理SSE流式响应
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-sse-");
        executor.initialize();

        configurer.setTaskExecutor(executor);
        // 设置异步请求超时时间（30分钟，适合长时间流式对话）
        configurer.setDefaultTimeout(30 * 60 * 1000L);
    }
}
