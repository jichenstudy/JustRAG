package com.shujichen.rag.common.mineru.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.configurationprocessor.json.JSONArray;

import java.util.Map;

/**
 * 文件解析返回体
 */
@Data
@Builder
public class FileParseResult {

    /**
     * 后端类型
     */
    private String backend;

    /**
     * 版本
     */
    private String version;

    /**
     * 返回内容
     */
    private Map<String, FileParseResultDict> results;

    @Data
    public static class FileParseResultDict {

        private String md_content;

        private JSONArray content_list;

        private Map<String, String> images;

    }

}
