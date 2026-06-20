package com.shujichen.rag.controller.file;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.vo.file.FileInfoVO;
import com.shujichen.rag.util.MinioUtil;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MinIO文件管理Controller
 */
@RestController
@RequestMapping("/api/file/minio")
public class MinioController {

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 简单文件上传
     *
     * @param file 文件
     * @return 文件信息
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> fileInfo = minioUtil.uploadFile(file);
            if (fileInfo != null) {
                return Result.success(fileInfo);
            } else {
                return Result.error("上传失败");
            }
        } catch (Exception e) {
            return Result.error("上传异常：" + e.getMessage());
        }
    }

    /**
     * 批量文件上传
     *
     * @param files 文件列表
     * @return 文件信息列表
     */
    @PostMapping("/upload/batch")
    public Result<List<Map<String, String>>> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<Map<String, String>> fileInfos = minioUtil.uploadFiles(files);
            return Result.success(fileInfos);
        } catch (Exception e) {
            return Result.error("上传异常：" + e.getMessage());
        }
    }

    /**
     * 文件下载
     *
     * @param fileName 文件名
     * @return 文件内容
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("fileName") String fileName) {
        try {
            InputStream inputStream = minioUtil.downloadFile(fileName);
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 文件预览
     *
     * @param fileName 文件名
     * @return 预览URL
     */
    @GetMapping("/preview/{fileName}")
    public Result<String> previewFile(@PathVariable("fileName") String fileName) {
        try {
            String url = minioUtil.getObjectUrl(fileName, 1);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("获取异常：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 操作结果
     */
    @DeleteMapping("/delete/{fileName}")
    public Result<Void> deleteFile(@PathVariable("fileName") String fileName) {
        try {
            minioUtil.deleteFile(fileName);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除异常：" + e.getMessage());
        }
    }

    /**
     * 列出所有文件
     *
     * @return 文件列表
     */
    @GetMapping("/list")
    public Result<List<Item>> listFiles() {
        try {
            List<Item> items = minioUtil.listObjects();
            return Result.success(items);
        } catch (Exception e) {
            return Result.error("获取异常：" + e.getMessage());
        }
    }

    /**
     * 初始化分片上传（预签名URL方式）
     *
     * @param fileName   文件名
     * @param partCount  分片数量
     * @param totalSize  文件总大小
     * @return 上传ID和上传URL列表
     */
    @PostMapping("/multipart/init")
    public Result<Map<String, Object>> initMultipartUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("partCount") int partCount,
            @RequestParam("totalSize") long totalSize) {
        try {
            // 后端自动获取和生成需要的信息
            String originalFileName = fileName; // 使用传入的fileName作为原始文件名
            String contentType = getContentTypeByExtension(fileName); // 根据扩展名推断Content-Type
            String fileHash = generateSimpleHash(fileName, totalSize); // 生成简单哈希（基于文件名和大小）

            Map<String, Object> initResult = minioUtil.initMultipartUpload(fileName, originalFileName, partCount, totalSize, contentType, fileHash);
            String uploadId = (String) initResult.get("uploadId");

            // 获取所有分片的预签名上传URL
            List<Map<String, Object>> uploadUrls = minioUtil.getMultipartUploadUrls(
                    minioUtil.getDefaultBucketName(), fileName, uploadId, partCount, 3600);

            Map<String, Object> result = new HashMap<>();
            result.put("uploadId", uploadId);
            result.put("uploadUrls", uploadUrls);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("初始化异常：" + e.getMessage());
        }
    }

    /**
     * 获取单个分片的预签名上传URL
     *
     * @param fileName    文件名
     * @param uploadId    上传ID
     * @param partNumber  分片编号
     * @return 上传URL
     */
    @PostMapping("/multipart/url")
    public Result<Map<String, Object>> getPartUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber) {
        try {
            String uploadUrl = minioUtil.getPresignedPartUploadUrl(fileName, partNumber, uploadId, 3600);

            Map<String, Object> result = new HashMap<>();
            result.put("partNumber", partNumber);
            result.put("uploadUrl", uploadUrl);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取URL异常：" + e.getMessage());
        }
    }

    /**
     * 记录分片上传完成
     *
     * @param uploadId    上传ID
     * @param partNumber  分片编号
     * @return 是否全部完成
     */
    @PostMapping("/multipart/part/complete")
    public Result<Map<String, Object>> recordPartComplete(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber) {
        try {
            minioUtil.recordPartUploadComplete(uploadId, partNumber);

            // 检查是否所有分片都已上传完成
            boolean allCompleted = minioUtil.isAllPartsUploaded(uploadId);

            Map<String, Object> result = new HashMap<>();
            result.put("allCompleted", allCompleted);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("记录分片异常：" + e.getMessage());
        }
    }

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
            FileInfoVO fileInfo = minioUtil.completeMultipartUpload(uploadId);
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error("分片合并异常：" + e.getMessage());
        }
    }

    /**
     * 获取分片上传状态
     *
     * @param uploadId 上传ID
     * @return 上传信息
     */
    @GetMapping("/multipart/status/{uploadId}")
    public Result<Map<String, Object>> getUploadStatus(@PathVariable("uploadId") String uploadId) {
        try {
            Map<String, Object> uploadInfo = minioUtil.getMultipartUploadInfo(uploadId);
            if (uploadInfo == null) {
                return Result.error("上传记录不存在");
            }
            return Result.success(uploadInfo);
        } catch (Exception e) {
            return Result.error("获取状态异常：" + e.getMessage());
        }
    }

    /**
     * 取消分片上传
     *
     * @param uploadId 上传ID
     * @return 操作结果
     */
    @PostMapping("/multipart/abort")
    public Result<String> abortMultipartUpload(@RequestParam("uploadId") String uploadId) {
        try {
            minioUtil.abortMultipartUpload(uploadId);
            return Result.success("取消上传成功");
        } catch (Exception e) {
            return Result.error("取消上传异常：" + e.getMessage());
        }
    }

    /**
     * 根据文件扩展名获取Content-Type
     *
     * @param fileName 文件名
     * @return Content-Type
     */
    private String getContentTypeByExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }

        String extension = fileName.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".pdf")) {
            return "application/pdf";
        } else if (extension.endsWith(".txt")) {
            return "text/plain";
        } else if (extension.endsWith(".zip")) {
            return "application/zip";
        } else if (extension.endsWith(".mp4")) {
            return "video/mp4";
        } else if (extension.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (extension.endsWith(".doc")) {
            return "application/msword";
        } else if (extension.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (extension.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 生成MD5哈希值（基于文件名和大小）
     *
     * @param fileName  文件名
     * @param totalSize 文件大小
     * @return MD5哈希值
     */
    private String generateSimpleHash(String fileName, long totalSize) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String input = fileName + totalSize;
            byte[] hashBytes = md.digest(input.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5生成报错");
        }
    }
}