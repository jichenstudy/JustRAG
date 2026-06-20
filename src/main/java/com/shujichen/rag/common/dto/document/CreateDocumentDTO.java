package com.shujichen.rag.common.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建文档请求DTO
 */
@Data
public class CreateDocumentDTO {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文件ID
     */
    @NotNull(message = "文件ID不能为空")
    private String fileId;

    /**
     * 文档名称
     */
    @NotBlank(message = "文档名称不能为空")
    private String name;

    /**
     * 文档类型
     */
    @NotBlank(message = "文档类型不能为空")
    private String docType;
}