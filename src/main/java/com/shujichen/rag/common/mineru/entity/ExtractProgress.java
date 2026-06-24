package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析进度信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractProgress {

    /**
     * 文档已解析页数，当 state=running 时有效
     */
    @JsonProperty("extracted_pages")
    private Integer extractedPages;

    /**
     * 文档总页数，当 state=running 时有效
     */
    @JsonProperty("total_pages")
    private Integer totalPages;

    /**
     * 文档解析开始时间，当 state=running 时有效
     */
    @JsonProperty("start_time")
    private String startTime;
}
