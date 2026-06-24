package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量任务创建响应数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchTaskResponse {

    /**
     * 批量提取任务 ID，可用于批量查询解析结果
     */
    @JsonProperty("batch_id")
    private String batchId;
}
