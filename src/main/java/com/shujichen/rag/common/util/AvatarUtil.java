package com.shujichen.rag.common.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 获取头像工具类
 */
public class AvatarUtil {

    private static final String AVATAR_BASE_URL = "https://avatar.exlb.net";

    /**
     * 根据用户名获取头像URL
     *
     * @param username 用户名
     * @return 头像URL
     */
    public static String getAvatarByUsername(String username) {
        try {
            String requestUrl = AVATAR_BASE_URL + "/" + username;
            HttpResponse response = HttpRequest.get(requestUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                    .header("Upgrade-Insecure-Requests", "1")
                    .execute();

            String location = response.header("Location");
            if (location != null && !location.isEmpty()) {
                return AVATAR_BASE_URL + location;
            }
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }

        return StringUtils.EMPTY;
    }

}
