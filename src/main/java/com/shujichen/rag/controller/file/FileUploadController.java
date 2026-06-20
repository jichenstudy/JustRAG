package com.shujichen.rag.controller.file;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.vo.file.FileInfoVO;
import com.shujichen.rag.service.FileDetailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件上传Controller
 */
@RestController
@RequestMapping("/api/file/fileUpload")
@RequiredArgsConstructor
@Tag(name = "文件上传控制器")
public class FileUploadController {

    private final FileDetailService fileDetailService;

    /**
     * 完成分片上传并合并
     *
     * @param uploadId 上传ID
     * @return 文件信息VO
     */
    @PostMapping("/multipart/complete")
    public Result<FileInfoVO> completeMultipartUpload(
            @RequestParam("uploadId") String uploadId) {
        try {
            return Result.success(fileDetailService.completeMultipartUpload(uploadId));
        } catch (Exception e) {
            return Result.error("分片合并异常：" + e.getMessage());
        }
    }
}