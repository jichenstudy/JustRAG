package com.shujichen.rag.builtin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujichen.rag.builtin.BuiltinTool;
import com.shujichen.rag.common.util.AvatarUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 随机头像生成工具
 * <p>
 * 根据输入字符串作为种子生成随机头像 URL，输入为空时使用随机字符串
 */
@Slf4j
@Component
public class RandomAvatarTool implements BuiltinTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> STYLES = List.of(
            "avataaars", "bottts", "pixel-art", "identicon",
            "thumbs", "fun-emoji", "lorelei", "notionists", "big-smile"
    );

    @Override
    public String getName() {
        return "random_avatar";
    }

    @Override
    public String getDescription() {
        return "生成随机头像URL。输入一个描述字符串作为种子（如 今天心情不错），相同种子会生成相同头像。不输入则随机生成";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "seed": {
                      "type": "string",
                      "description": "头像种子字符串，如 今天心情不错"
                    }
                  }
                }
                """;
    }

    @Override
    public String execute(String input) {
        try {
            String seed;
            if (input != null && !input.isBlank()) {
                Map<String, Object> params = MAPPER.readValue(input, Map.class);
                seed = params.getOrDefault("seed", "").toString();
            } else {
                seed = "";
            }

            if (seed.isBlank()) {
                seed = UUID.randomUUID().toString().substring(0, 8);
            }

            // 随机选一个风格
            String style = STYLES.get(Math.abs(seed.hashCode()) % STYLES.size());

            // DiceBear API
            String avatarUrl = String.format("https://api.dicebear.com/9.x/%s/svg?seed=%s", style, seed);

            // 也尝试用 AvatarUtil 获取备用头像
            String altAvatar = AvatarUtil.getAvatarByUsername(seed);

            log.info("生成头像: seed={}, style={}, url={}", seed, style, avatarUrl);

            return MAPPER.writeValueAsString(Map.of(
                    "success", true,
                    "seed", seed,
                    "style", style,
                    "url", avatarUrl,
                    "altUrl", altAvatar
            ));
        } catch (Exception e) {
            log.error("头像生成失败", e);
            return error("头像生成失败: " + e.getMessage());
        }
    }

    private String error(String msg) {
        try {
            return MAPPER.writeValueAsString(Map.of("success", false, "error", msg));
        } catch (Exception ex) {
            return "{\"success\":false,\"error\":\"" + msg + "\"}";
        }
    }
}
