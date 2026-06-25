package com.shujichen.rag.common.dto.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传请求DTO
 */
@Data
public class FileDTO {

    /**
     * 上传文件列表
     */
    public List<MultipartFile> files;

    /**
     * 知识库ID
     */
    public Long knowledgeBaseId;
}
