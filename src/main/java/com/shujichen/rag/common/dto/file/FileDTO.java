package com.shujichen.rag.common.dto.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求DTO
 */
@Data
public class FileDTO {

    /**
     * 上传文件
     */
    public MultipartFile file;

    /**
     * 知识库ID
     */
    public Long knowledgeBaseId;

    /**
     * 是否解析
     */
    public Boolean isParse;
}