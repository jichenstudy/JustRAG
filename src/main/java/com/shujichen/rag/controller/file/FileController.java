package com.shujichen.rag.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.oss.core.OssClient;
import com.shujichen.rag.common.oss.entity.UploadResult;
import com.shujichen.rag.common.oss.factory.OssFactory;
import com.shujichen.rag.entity.FileDetail;
import com.shujichen.rag.service.FileDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件管理Controller
 */
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileDetailService fileDetailService;

    /**
     * 文件上传
     *
     * @param files 文件列表
     * @return 文件信息列表
     */
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<List<Map<String, String>>> uploadFile(
            @Parameter(description = "文件列表") @RequestParam("files") List<MultipartFile> files) {
        try {
            List<Map<String, String>> fileInfos = files.stream()
                    .map(file -> {
                        try {
                            // 获取原始文件名
                            String originalFilename = file.getOriginalFilename();
                            // 生成文件扩展名
                            String extension = getExtension(originalFilename);
                            // 生成唯一文件名
                            String fileName = IdUtil.fastSimpleUUID() + extension;
                            // 获取文件内容类型
                            String contentType = file.getContentType();

                            // 使用 OssFactory 获取 OssClient 实例
                            OssClient ossClient = OssFactory.instance();

                            // 上传文件
                            UploadResult uploadResult = ossClient.uploadSuffix(
                                    file.getInputStream(),
                                    fileName,
                                    file.getSize(),
                                    contentType
                            );

                            // 保存文件记录到数据库
                            FileDetail fileDetail = new FileDetail()
                                    .setUrl(uploadResult.getUrl())
                                    .setSize(file.getSize())
                                    .setFilename(fileName)
                                    .setOriginalFilename(originalFilename)
                                    .setBucketName(ossClient.getBucketName())
                                    .setObjectName(uploadResult.getFilename())
                                    .setBasePath(fileName)
                                    .setPath(uploadResult.getFilename())
                                    .setExt(extension)
                                    .setContentType(contentType)
                                    .setPlatform(ossClient.getConfigKey())
                                    .setHashInfo(calculateMd5(file))
                                    .setUploadStatus(1)
                                    .setUserId(StpUtil.getLoginIdAsLong())
                                    .setCreateTime(LocalDateTime.now());

                            fileDetailService.insert(fileDetail);

                            // 返回文件信息
                            Map<String, String> fileInfo = new HashMap<>();
                            fileInfo.put("fileId", fileDetail.getId().toString());
                            fileInfo.put("fileName", fileName);
                            fileInfo.put("originalFilename", originalFilename);
                            fileInfo.put("url", uploadResult.getUrl());
                            fileInfo.put("size", String.valueOf(file.getSize()));
                            fileInfo.put("contentType", contentType);

                            return fileInfo;
                        } catch (Exception e) {
                            log.error("上传文件失败", e);
                            throw new RuntimeException("上传失败：" + e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());

            return Result.success(fileInfos);
        } catch (Exception e) {
            log.error("批量上传文件失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 文件下载
     *
     * @param fileName 文件名
     * @return 文件内容
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "文件名") @PathVariable("fileName") String fileName) {
        try {
            // 使用 OssFactory 获取 OssClient 实例
            OssClient ossClient = OssFactory.instance();

            // 下载文件到字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ossClient.download(fileName, outputStream, null);
            byte[] bytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return new ResponseEntity<>(null, null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 文件预览
     *
     * @param fileName 文件名
     * @return 预览URL
     */
    @Operation(summary = "预览文件")
    @GetMapping("/preview/{fileName}")
    public Result<String> previewFile(
            @Parameter(description = "文件名") @PathVariable("fileName") String fileName) {
        try {
            // 使用 OssFactory 获取 OssClient 实例
            OssClient ossClient = OssFactory.instance();

            // 获取文件URL
            String url = ossClient.getUrl() + "/" + fileName;
            return Result.success(url);
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            return Result.error("获取异常：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 操作结果
     */
    @Operation(summary = "删除文件")
    @DeleteMapping("/delete/{fileName}")
    public Result<Void> deleteFile(
            @Parameter(description = "文件名") @PathVariable("fileName") String fileName) {
        try {
            // 使用 OssFactory 获取 OssClient 实例
            OssClient ossClient = OssFactory.instance();

            // 删除文件
            ossClient.delete(fileName);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除异常：" + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 计算文件 MD5
     */
    private String calculateMd5(MultipartFile file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算 MD5 失败", e);
            return null;
        }
    }
}
