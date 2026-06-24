package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量文件上传响应数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchUploadResponse {

    /**
     * 批量提取任务 ID，可用于批量查询解析结果
     */
    @JsonProperty("batch_id")
    private String batchId;

    /**
     * 文件上传链接列表
     */
    @JsonProperty("file_urls")
    private List<String> fileUrls;
}
