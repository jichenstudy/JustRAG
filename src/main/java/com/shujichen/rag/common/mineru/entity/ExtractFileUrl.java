package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 包含 URL 的解析文件信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractFileUrl {

    /**
     * 文件链接，支持.pdf、.doc、.docx、.ppt、.pptx、.xls、.xlsx、图片（png/jpg/jpeg/jp2/webp/gif/bmp）、.html多种格式
     */
    private String url;

    /**
     * 是否启动 OCR 功能，默认 false
     */
    @Builder.Default
    @JsonProperty("is_ocr")
    private Boolean isOcr = false;

    /**
     * 解析对象对应的数据 ID，可用于唯一标识业务数据
     */
    @JsonProperty("data_id")
    private String dataId;

    /**
     * 指定页码范围，格式为逗号分隔的字符串
     */
    @JsonProperty("page_ranges")
    private String pageRanges;
}
