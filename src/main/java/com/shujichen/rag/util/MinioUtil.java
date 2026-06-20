package com.shujichen.rag.util;

import com.shujichen.rag.common.vo.file.FileInfoVO;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final RedisUtil redisUtil;

    @Value("${minio.bucket-name}")
    private String defaultBucketName;

    /**
     * 获取默认存储桶名称
     *
     * @return 默认存储桶名称
     */
    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    @SneakyThrows
    public boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    @SneakyThrows
    public void makeBucket(String bucketName) {
        if (bucketExists(bucketName)) {
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }

    /**
     * 获取所有存储桶
     *
     * @return 存储桶列表
     */
    @SneakyThrows
    public List<Bucket> listBuckets() {
        return minioClient.listBuckets();
    }

    /**
     * 删除存储桶
     *
     * @param bucketName 存储桶名称
     */
    @SneakyThrows
    public void removeBucket(String bucketName) {
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }

    /**
     * 简单文件上传
     *
     * @param file       文件
     * @param bucketName 存储桶名称
     * @return 文件信息
     */
    @SneakyThrows
    public Map<String, String> uploadFile(MultipartFile file, String bucketName) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (!bucketExists(bucketName)) {
            makeBucket(bucketName);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        // 进行非空判断
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + fileExtension;

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .contentType(file.getContentType())
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("fileName", fileName);
        resultMap.put("originalFilename", originalFilename);
        resultMap.put("url", getObjectUrl(bucketName, fileName, 7));

        return resultMap;
    }

    /**
     * 简单文件上传（使用默认存储桶）
     *
     * @param file 文件
     * @return 文件信息
     */
    public Map<String, String> uploadFile(MultipartFile file) {
        return uploadFile(file, defaultBucketName);
    }

    /**
     * 批量文件上传
     *
     * @param files      文件列表
     * @param bucketName 存储桶名称
     * @return 文件信息列表
     */
    public List<Map<String, String>> uploadFiles(List<MultipartFile> files, String bucketName) {
        return files.stream()
                .map(file -> uploadFile(file, bucketName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 批量文件上传（使用默认存储桶）
     *
     * @param files 文件列表
     * @return 文件信息列表
     */
    public List<Map<String, String>> uploadFiles(List<MultipartFile> files) {
        return uploadFiles(files, defaultBucketName);
    }

    /**
     * 下载文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 输入流
     */
    @SneakyThrows
    public InputStream downloadFile(String bucketName, String objectName) {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 下载文件（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @return 输入流
     */
    public InputStream downloadFile(String objectName) {
        return downloadFile(defaultBucketName, objectName);
    }

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    @SneakyThrows
    public void deleteFile(String bucketName, String objectName) {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 删除文件（使用默认存储桶）
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        deleteFile(defaultBucketName, objectName);
    }

    /**
     * 批量删除文件
     *
     * @param bucketName  存储桶名称
     * @param objectNames 对象名称列表
     * @return 删除错误列表
     */
    @SneakyThrows
    public List<DeleteError> deleteFiles(String bucketName, List<String> objectNames) {
        List<DeleteObject> objects = objectNames.stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(objects)
                .build());

        List<DeleteError> errors = new ArrayList<>();
        for (Result<DeleteError> result : results) {
            errors.add(result.get());
        }
        return errors;
    }

    /**
     * 批量删除文件（使用默认存储桶）
     *
     * @param objectNames 对象名称列表
     * @return 删除错误列表
     */
    public List<DeleteError> deleteFiles(List<String> objectNames) {
        return deleteFiles(defaultBucketName, objectNames);
    }

    /**
     * 获取文件URL
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires    过期时间（天）
     * @return 文件URL
     */
    @SneakyThrows
    public String getObjectUrl(String bucketName, String objectName, int expires) {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectName)
                .expiry(expires, TimeUnit.DAYS)
                .build());
    }

    /**
     * 获取文件URL（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @param expires    过期时间（天）
     * @return 文件URL
     */
    public String getObjectUrl(String objectName, int expires) {
        return getObjectUrl(defaultBucketName, objectName, expires);
    }

    /**
     * 检查文件是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 是否存在
     */
    @SneakyThrows
    public boolean objectExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查文件是否存在（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean objectExists(String objectName) {
        return objectExists(defaultBucketName, objectName);
    }

    /**
     * 列出存储桶中的所有对象
     *
     * @param bucketName 存储桶名称
     * @return 对象列表
     */
    @SneakyThrows
    public List<Item> listObjects(String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build());

        List<Item> items = new ArrayList<>();
        for (Result<Item> result : results) {
            items.add(result.get());
        }
        return items;
    }

    /**
     * 列出存储桶中的所有对象（使用默认存储桶）
     *
     * @return 对象列表
     */
    public List<Item> listObjects() {
        return listObjects(defaultBucketName);
    }

    /**
     * 大文件上传（使用Minio的自动分片功能）
     *
     * @param bucketName  存储桶名称
     * @param objectName  对象名称
     * @param stream      输入流
     * @param size        文件大小
     * @param contentType 文件类型
     */
    @SneakyThrows
    public void uploadLargeFile(String bucketName, String objectName, InputStream stream, long size, String contentType) {
        if (!bucketExists(bucketName)) {
            makeBucket(bucketName);
        }

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(stream, size, -1)
                .contentType(contentType)
                .build());
    }

    /**
     * 大文件上传（使用默认存储桶）
     *
     * @param objectName  对象名称
     * @param stream      输入流
     * @param size        文件大小
     * @param contentType 文件类型
     */
    public void uploadLargeFile(String objectName, InputStream stream, long size, String contentType) {
        uploadLargeFile(defaultBucketName, objectName, stream, size, contentType);
    }

    /**
     * 获取分片上传的预签名URL
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param partNumber 分片编号
     * @param uploadId   上传ID
     * @param expires    过期时间（秒）
     * @return 预签名URL
     */
    @SneakyThrows
    public String getPresignedPartUploadUrl(String bucketName, String objectName, int partNumber, String uploadId, int expires) {
        // 使用临时分片对象名称
        String partObjectName = getPartObjectName(objectName, uploadId, partNumber);

        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .object(partObjectName)
                .expiry(expires, TimeUnit.SECONDS)
                .build());
    }

    /**
     * 获取分片上传的预签名URL（使用默认存储桶）
     *
     * @param objectName 对象名称
     * @param partNumber 分片编号
     * @param uploadId   上传ID
     * @param expires    过期时间（秒）
     * @return 预签名URL
     */
    public String getPresignedPartUploadUrl(String objectName, int partNumber, String uploadId, int expires) {
        return getPresignedPartUploadUrl(defaultBucketName, objectName, partNumber, uploadId, expires);
    }

    /**
     * 初始化分片上传（使用预签名URL方式）
     *
     * @param bucketName       存储桶名称
     * @param objectName       对象名称
     * @param originalFileName 原始文件名
     * @param partCount        分片总数
     * @param totalSize        文件总大小
     * @param contentType      文件类型
     * @param fileHash         文件Hash值
     * @return 分片上传信息
     */
    @SneakyThrows
    public Map<String, Object> initMultipartUpload(String bucketName, String objectName, String originalFileName,
                                                   int partCount, long totalSize, String contentType, String fileHash) {
        if (!bucketExists(bucketName)) {
            makeBucket(bucketName);
        }

        // 生成一个唯一的uploadId
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        // 获取文件扩展名
        String fileExtension = getFileExtension(originalFileName);

        // 在Redis中存储分片上传信息
        String redisKey = "multipart_upload:" + uploadId;
        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("uploadId", uploadId);
        uploadInfo.put("bucketName", bucketName);
        uploadInfo.put("objectName", objectName);
        uploadInfo.put("originalFileName", originalFileName);
        uploadInfo.put("fileExtension", fileExtension);
        uploadInfo.put("partCount", partCount);
        uploadInfo.put("totalSize", totalSize);
        uploadInfo.put("contentType", contentType);
        uploadInfo.put("fileHash", fileHash);
        uploadInfo.put("completedParts", new HashSet<Integer>());
        uploadInfo.put("uploadTime", System.currentTimeMillis());
        uploadInfo.put("status", "INITIATED");

        redisUtil.set(redisKey, uploadInfo, 1, TimeUnit.HOURS);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("bucketName", bucketName);
        result.put("objectName", objectName);

        return result;
    }

    /**
     * 初始化分片上传（使用默认存储桶）
     *
     * @param objectName       对象名称
     * @param originalFileName 原始文件名
     * @param partCount        分片总数
     * @param totalSize        文件总大小
     * @param contentType      文件类型
     * @param fileHash         文件Hash值
     * @return 分片上传信息
     */
    public Map<String, Object> initMultipartUpload(String objectName, String originalFileName, int partCount, long totalSize, String contentType, String fileHash) {
        return initMultipartUpload(defaultBucketName, objectName, originalFileName, partCount, totalSize, contentType, fileHash);
    }

    /**
     * 获取多个分片的预签名上传URL
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param uploadId   上传ID
     * @param partCount  分片数量
     * @param expires    过期时间（秒）
     * @return 预签名URL列表
     */
    public List<Map<String, Object>> getMultipartUploadUrls(String bucketName, String objectName, String uploadId, int partCount, int expires) {
        List<Map<String, Object>> urls = new ArrayList<>();

        for (int i = 1; i <= partCount; i++) {
            String presignedUrl = getPresignedPartUploadUrl(bucketName, objectName, i, uploadId, expires);
            Map<String, Object> urlInfo = new HashMap<>();
            urlInfo.put("partNumber", i);
            urlInfo.put("url", presignedUrl);
            urls.add(urlInfo);
        }

        return urls;
    }

    /**
     * 记录分片上传完成
     *
     * @param uploadId   上传ID
     * @param partNumber 分片编号
     */
    @SuppressWarnings("unchecked")
    public void recordPartUploadComplete(String uploadId, int partNumber) {
        String redisKey = "multipart_upload:" + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.get(redisKey);

        if (uploadInfo != null) {
            Object completedPartsObj = uploadInfo.get("completedParts");
            Set<Integer> completedParts = new HashSet<>();

            // 处理Redis反序列化时可能返回List的情况
            if (completedPartsObj instanceof Set) {
                completedParts = (Set<Integer>) completedPartsObj;
            } else if (completedPartsObj instanceof List) {
                List<Integer> partsList = (List<Integer>) completedPartsObj;
                completedParts = new HashSet<>(partsList);
            }

            completedParts.add(partNumber);
            uploadInfo.put("completedParts", completedParts);

            // 更新Redis
            redisUtil.set(redisKey, uploadInfo, 1, TimeUnit.HOURS);
        }
    }

    /**
     * 检查所有分片是否上传完成
     *
     * @param uploadId 上传ID
     * @return 是否完成
     */
    @SuppressWarnings("unchecked")
    public boolean isAllPartsUploaded(String uploadId) {
        String redisKey = "multipart_upload:" + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.get(redisKey);

        if (uploadInfo == null) {
            return false;
        }

        Integer totalParts = (Integer) uploadInfo.get("partCount");
        Object completedPartsObj = uploadInfo.get("completedParts");

        // 处理Redis反序列化时可能返回List的情况
        int completedCount = 0;
        if (completedPartsObj instanceof Set) {
            Set<Integer> completedParts = (Set<Integer>) completedPartsObj;
            completedCount = completedParts.size();
        } else if (completedPartsObj instanceof List) {
            List<Integer> completedParts = (List<Integer>) completedPartsObj;
            completedCount = completedParts.size();
        }

        return totalParts != null && completedCount == totalParts;
    }

    /**
     * 获取分片上传信息
     *
     * @param uploadId 上传ID
     * @return 上传信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMultipartUploadInfo(String uploadId) {
        String redisKey = "multipart_upload:" + uploadId;
        return (Map<String, Object>) redisUtil.get(redisKey);
    }

    /**
     * 完成分片上传并合并文件
     *
     * @param uploadId 上传ID
     * @return 合并后的文件信息
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public FileInfoVO completeMultipartUpload(String uploadId) {
        String redisKey = "multipart_upload:" + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.get(redisKey);

        if (uploadInfo == null) {
            throw new RuntimeException("分片上传信息不存在，uploadId: " + uploadId);
        }

        String bucketName = (String) uploadInfo.get("bucketName");
        String objectName = (String) uploadInfo.get("objectName");
        String originalFileName = (String) uploadInfo.get("originalFileName");
        String fileExtension = (String) uploadInfo.get("fileExtension");
        String contentType = (String) uploadInfo.get("contentType");
        String fileHash = (String) uploadInfo.get("fileHash");
        Integer partCount = (Integer) uploadInfo.get("partCount");
        Long totalSize = ((Number) uploadInfo.get("totalSize")).longValue();
        Object completedPartsObj = uploadInfo.get("completedParts");

        // 处理Redis反序列化时可能返回List的情况
        int completedCount = 0;
        if (completedPartsObj instanceof Set) {
            Set<Integer> completedParts = (Set<Integer>) completedPartsObj;
            completedCount = completedParts.size();
        } else if (completedPartsObj instanceof List) {
            List<Integer> completedParts = (List<Integer>) completedPartsObj;
            completedCount = completedParts.size();
        }

        // 检查所有分片是否都已上传
        if (completedCount != partCount) {
            throw new RuntimeException("还有分片未上传完成，已完成: " + completedCount + "/" + partCount);
        }

        try {
            // 准备要合并的分片列表
            List<ComposeSource> sources = new ArrayList<>();
            for (int i = 1; i <= partCount; i++) {
                String partObjectName = getPartObjectName(objectName, uploadId, i);
                sources.add(ComposeSource.builder()
                        .bucket(bucketName)
                        .object(partObjectName)
                        .build());
            }

            // 使用composeObject合并分片
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .sources(sources)
                    .build());

            // 删除临时分片文件
            cleanupTempParts(bucketName, objectName, uploadId, partCount);

            // 清除Redis缓存
            redisUtil.delete(redisKey);

            // 记录完成时间用于返回
            Long completeTime = System.currentTimeMillis();

            // 构建并返回FileInfoVO
            return FileInfoVO.builder()
                    .uploadId(uploadId)
                    .fileName(objectName)
                    .objectName(objectName)
                    .originalFileName(originalFileName)
                    .fileExtension(fileExtension)
                    .fileSize(totalSize)
                    .fileSizeFormatted(formatFileSize(totalSize))
                    .contentType(contentType)
                    .fileHash(fileHash)
                    .bucketName(bucketName)
                    .url(getObjectUrl(bucketName, objectName, 7))
                    .status("COMPLETED")
                    .partCount(partCount)
                    .completedParts(completedCount)
                    .uploadTime(timestampToLocalDateTime((Long) uploadInfo.get("uploadTime")))
                    .completeTime(timestampToLocalDateTime(completeTime))
                    .build();

        } catch (Exception e) {
            // 合并失败，更新状态
            uploadInfo.put("status", "FAILED");
            uploadInfo.put("errorMessage", e.getMessage());
            redisUtil.set(redisKey, uploadInfo, 1, TimeUnit.HOURS);
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取分片的临时对象名称
     *
     * @param originalObjectName 原始对象名
     * @param uploadId           上传ID
     * @param partNumber         分片编号
     * @return 分片对象名
     */
    private String getPartObjectName(String originalObjectName, String uploadId, int partNumber) {
        return "temp_parts/" + uploadId + "/" + originalObjectName + ".part" + partNumber;
    }

    /**
     * 清理临时分片文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param uploadId   上传ID
     * @param partCount  分片数量
     */
    @SneakyThrows
    private void cleanupTempParts(String bucketName, String objectName, String uploadId, int partCount) {
        List<String> tempObjects = new ArrayList<>();
        for (int i = 1; i <= partCount; i++) {
            tempObjects.add(getPartObjectName(objectName, uploadId, i));
        }

        try {
            deleteFiles(bucketName, tempObjects);
        } catch (Exception e) {
            // 清理失败不影响主流程，只记录日志
            System.err.println("清理临时分片文件失败: " + e.getMessage());
        }
    }

    /**
     * 取消分片上传
     *
     * @param uploadId 上传ID
     */
    @SuppressWarnings("unchecked")
    public void abortMultipartUpload(String uploadId) {
        String redisKey = "multipart_upload:" + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.get(redisKey);

        if (uploadInfo != null) {
            String bucketName = (String) uploadInfo.get("bucketName");
            String objectName = (String) uploadInfo.get("objectName");
            Integer partCount = (Integer) uploadInfo.get("partCount");

            // 清理已上传的临时分片
            try {
                cleanupTempParts(bucketName, objectName, uploadId, partCount);
            } catch (Exception e) {
                System.err.println("清理临时分片失败: " + e.getMessage());
            }

            // 清除Redis缓存
            redisUtil.delete(redisKey);
        }
    }

    /**
     * 生成文件哈希值（用于秒传判断）
     *
     * @param file 文件
     * @return 哈希值
     */
    @SneakyThrows
    public String generateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = file.getBytes();
            byte[] digest = md.digest(bytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 如果MD5计算失败，使用简单的哈希值
            return file.getSize() + "-" + file.getOriginalFilename().hashCode();
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（包含点号）
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex != -1 ? fileName.substring(lastDotIndex) : "";
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
     */
    public String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 时间戳转LocalDateTime
     *
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    private LocalDateTime timestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
