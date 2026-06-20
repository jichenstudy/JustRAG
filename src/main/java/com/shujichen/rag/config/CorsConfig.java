package com.shujichen.rag.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS跨域配置类
 */
@Component
public class CorsConfig implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    /**
     * 处理跨域请求
     *
     * @param servletRequest  请求对象
     * @param servletResponse 响应对象
     * @param filterChain     过滤器链
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (httpResponse.getHeader("Access-Control-Allow-Origin") == null) {
            String origin = httpRequest.getHeader("Origin");
            if (origin != null && origin.contains("localhost")) {
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            } else {
                httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            }
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin, User-Agent, Cache-Control, X-Requested-With");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }

        if ("OPTIONS".equals(httpRequest.getMethod())) {
            httpResponse.setStatus(200);
            return;
        }
        filterChain.doFilter(servletRequest, httpResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}