package com.shujichen.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    private final int corePoolSize = 50;

    /**
     * 最大线程数
     */
    private final int maxPoolSize = 200;

    /**
     * 队列容量
     */
    private final int queueCapacity = 1000;

    /**
     * 线程存活时间（秒）
     */
    private final int keepAliveSeconds = 300;

    /**
     * 配置线程池任务执行器
     *
     * @return 线程池任务执行器
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}