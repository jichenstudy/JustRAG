package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量文件上传解析请求
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchFileUploadRequest {

    /**
     * 是否开启公式识别，默认 true
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
     * 文件列表
     */
    private List<ExtractFile> files;

    /**
     * 解析结果回调通知 URL
     */
    private String callback;

    /**
     * 随机字符串，用于回调通知中的签名
     */
    private String seed;

    /**
     * 额外导出格式，支持docx、html、latex
     */
    @JsonProperty("extra_formats")
    private List<String> extraFormats;

    /**
     * Mineru模型版本，v1或v2，默认v1
     */
    @Builder.Default
    @JsonProperty("model_version")
    private String modelVersion = "v1";
}
