package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量结果查询响应数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResultResponse {

    /**
     * 批量任务 ID
     */
    @JsonProperty("batch_id")
    private String batchId;

    /**
     * 解析结果列表
     */
    @JsonProperty("extract_result")
    private List<BatchExtractResult> extractResult;
}
