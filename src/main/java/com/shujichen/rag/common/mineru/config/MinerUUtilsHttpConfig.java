package com.shujichen.rag.common.mineru.config;

import com.shujichen.rag.common.mineru.core.MinerUHttpService;
import com.shujichen.rag.common.util.SpringUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

/**
 * MinerU HTTP 客户端配置
 */
@Slf4j
@Configuration
public class MinerUUtilsHttpConfig {

    @Bean
    public MinerUHttpService minerUUtilsHttpService() {
        // 获取配置
        String serverUrl = SpringUtils.getProperty("mineru.server-url");
        String token = SpringUtils.getProperty("mineru.token");

        log.info("初始化 MinerU HTTP 客户端，服务地址: {}", serverUrl);

        // 创建 WebClient Builder
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(serverUrl)
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000) // 30秒连接超时
                    .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(3600)) // 1小时读取超时
                    )
            ))
            .codecs(configurer -> {
                // 设置请求和响应的最大接收参数大小
                configurer.defaultCodecs().maxInMemorySize(200 * 1024 * 1024); // 200MB
            });

        // 如果配置了 Token，则添加认证头
        if (StringUtils.hasText(token)) {
            log.info("MinerU 客户端已配置 Token 认证");
            builder.defaultHeader("Authorization", "Bearer " + token);
        } else {
            log.warn("MinerU 客户端未配置 Token，云端 API 功能将不可用");
        }

        WebClient client = builder.build();

        // 创建工厂
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(client))
            .build();

        // 获取代理对象
        return factory.createClient(MinerUHttpService.class);
    }

}
