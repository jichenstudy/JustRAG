package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个文件解析任务请求
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractTaskRequest {

    /**
     * 文件 URL，支持.pdf、.doc、.docx、.ppt、.pptx、.xls、.xlsx、图片（png/jpg/jpeg/jp2/webp/gif/bmp）、.html多种格式
     */
    private String url;

    /**
     * 是否启动 OCR 功能，默认 false
     */
    @Builder.Default
    @JsonProperty("is_ocr")
    private Boolean isOcr = false;

    /**
     * 是否开启公式识别，默认 true。对于vlm模型，这个参数指只会影响行内公式的解析
     */
    @Builder.Default
    @JsonProperty("enable_formula")
    private Boolean enableFormula = true;

    /**
     * 是否开启表格识别，默认 true
     */
    @Builder.Default
    @JsonProperty("enable_table")
    private Boolean enableTable = true;

    /**
     * 指定文档语言，默认 ch，可以设置为auto
     */
    @Builder.Default
    private String language = "ch";

    /**
     * 解析对象对应的数据 ID，可用于唯一标识业务数据
     */
    @JsonProperty("data_id")
    private String dataId;

    /**
     * 解析结果回调通知 URL，该字段为空时，必须定时轮询解析结果
     */
    private String callback;

    /**
     * 随机字符串，用于回调通知中的签名，当使用 callback 时，该字段必须提供
     */
    private String seed;

    /**
     * 额外导出格式，支持docx、html、latex
     */
    @JsonProperty("extra_formats")
    private List<String> extraFormats;

    /**
     * 指定页码范围，格式为逗号分隔的字符串
     */
    @JsonProperty("page_ranges")
    private String pageRanges;

    /**
     * Mineru模型版本，三个选项:pipeline、vlm、MinerU-HTML，默认pipeline
     */
    @Builder.Default
    @JsonProperty("model_version")
    private String modelVersion = "pipeline";
}
